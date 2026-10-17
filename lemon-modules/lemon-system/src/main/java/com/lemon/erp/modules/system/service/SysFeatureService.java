package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.tenant.PlatformFeatures;
import com.lemon.erp.modules.system.dto.FeatureCatalogView;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.entity.SysTenantPackage;
import com.lemon.erp.modules.system.entity.SysTenantPackageMenu;
import com.lemon.erp.modules.system.mapper.SysMenuMapper;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMapper;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 功能模块与行业模板（{@link SysFeatureCatalog}）：展开为菜单，按行业模板新建租户时匹配或创建套餐
 */
@Service
@RequiredArgsConstructor
public class SysFeatureService {

    private static final int NAME_MAX = 64;

    private final SysMenuMapper menuMapper;
    private final SysTenantPackageMapper packageMapper;
    private final SysTenantPackageMenuMapper packageMenuMapper;

    /** 可加入套餐的菜单（不含平台专属功能及其按钮） */
    public List<SysMenu> assignableMenus() {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort));
        Set<Long> excluded = menus.stream().filter(m -> PlatformFeatures.isPlatformOnly(m.getPerms()))
                .map(SysMenu::getId).collect(Collectors.toSet());
        return menus.stream()
                .filter(m -> !excluded.contains(m.getId()) && !excluded.contains(m.getParentId()))
                .toList();
    }

    public FeatureCatalogView catalog() {
        List<SysMenu> menus = assignableMenus();
        Map<Long, List<Long>> children = new HashMap<>();
        menus.forEach(m -> children.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m.getId()));
        Set<Long> assignable = menus.stream().map(SysMenu::getId).collect(Collectors.toSet());
        List<FeatureCatalogView.Feature> features = SysFeatureCatalog.FEATURES.stream()
                .map(f -> new FeatureCatalogView.Feature(f.key(), f.name(), f.group(), f.description(), f.required(), f.requires(),
                        List.copyOf(expand(f.menus(), children, assignable))))
                .toList();
        List<FeatureCatalogView.Industry> industries = SysFeatureCatalog.INDUSTRIES.stream()
                .map(i -> new FeatureCatalogView.Industry(i.key(), i.name(), i.description(), List.copyOf(withRequires(i.features()))))
                .toList();
        return new FeatureCatalogView(features, industries);
    }

    /**
     * 按行业模板新建租户：已有菜单完全相同的启用套餐时直接使用，否则以 name 新建套餐（重名时加上租户名称）
     *
     * @return 套餐 ID
     */
    public Long resolvePackage(String name, String tenantName, Collection<Long> menuIds) {
        Set<Long> menus = normalize(menuIds);
        if (menus.isEmpty()) {
            throw new BizException("请选择租户的功能");
        }
        Map<Long, Set<Long>> packageMenus = new HashMap<>();
        packageMenuMapper.selectList(Wrappers.lambdaQuery())
                .forEach(pm -> packageMenus.computeIfAbsent(pm.getPackageId(), k -> new HashSet<>()).add(pm.getMenuId()));
        for (SysTenantPackage pkg : packageMapper.selectList(Wrappers.<SysTenantPackage>lambdaQuery()
                .eq(SysTenantPackage::getStatus, 1).orderByAsc(SysTenantPackage::getId))) {
            if (menus.equals(packageMenus.get(pkg.getId()))) {
                return pkg.getId();
            }
        }
        String base = name == null || name.isBlank() ? "自定义" : name.trim();
        String packageName = uniqueName(base, tenantName);
        SysTenantPackage pkg = new SysTenantPackage();
        pkg.setName(packageName);
        pkg.setStatus(1);
        pkg.setRemark("新建租户「" + tenantName + "」时按功能模块创建");
        packageMapper.insert(pkg);
        packageMenuMapper.insertBatch(pkg.getId(), menus);
        return pkg.getId();
    }

    /** 只保留可分配的菜单，补齐上级目录 */
    Set<Long> normalize(Collection<Long> menuIds) {
        Map<Long, SysMenu> byId = assignableMenus().stream().collect(Collectors.toMap(SysMenu::getId, m -> m));
        Set<Long> result = new TreeSet<>();
        for (Long id : menuIds == null ? List.<Long>of() : menuIds) {
            SysMenu m = id == null ? null : byId.get(id);
            while (m != null && result.add(m.getId())) {
                m = byId.get(m.getParentId());
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- 工具

    /** 菜单及其全部下级 */
    private static Set<Long> expand(List<Long> roots, Map<Long, List<Long>> children, Set<Long> assignable) {
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (assignable.contains(id) && result.add(id)) {
                queue.addAll(children.getOrDefault(id, List.of()));
            }
        }
        return result;
    }

    /** 模块加上依赖的模块（递归）和必选模块 */
    static Set<String> withRequires(Collection<String> keys) {
        Map<String, SysFeatureCatalog.Feature> byKey = SysFeatureCatalog.FEATURES.stream()
                .collect(Collectors.toMap(SysFeatureCatalog.Feature::key, f -> f));
        Set<String> result = new LinkedHashSet<>();
        SysFeatureCatalog.FEATURES.stream().filter(SysFeatureCatalog.Feature::required).forEach(f -> result.add(f.key()));
        Deque<String> queue = new ArrayDeque<>(keys);
        while (!queue.isEmpty()) {
            SysFeatureCatalog.Feature f = byKey.get(queue.poll());
            if (f != null && result.add(f.key())) {
                queue.addAll(f.requires());
            }
        }
        // 按目录中的顺序返回
        return SysFeatureCatalog.FEATURES.stream().map(SysFeatureCatalog.Feature::key).filter(result::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String uniqueName(String base, String tenantName) {
        List<String> candidates = List.of(base, base + "（" + tenantName + "）");
        for (String c : candidates) {
            String name = c.length() > NAME_MAX ? c.substring(0, NAME_MAX) : c;
            if (!exists(name)) {
                return name;
            }
        }
        for (int i = 2; ; i++) {
            String suffix = "（" + i + "）";
            String name = (base.length() + suffix.length() > NAME_MAX ? base.substring(0, NAME_MAX - suffix.length()) : base) + suffix;
            if (!exists(name)) {
                return name;
            }
        }
    }

    private boolean exists(String name) {
        return packageMapper.selectCount(Wrappers.<SysTenantPackage>lambdaQuery().eq(SysTenantPackage::getName, name)) > 0;
    }

    /** 测试用：目录中引用的菜单 ID */
    static Set<Long> referencedMenuIds() {
        return SysFeatureCatalog.FEATURES.stream().flatMap(f -> f.menus().stream()).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
