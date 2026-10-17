package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.entity.SysTenantPackage;
import com.lemon.erp.modules.system.entity.SysTenantPackageMenu;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMapper;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户套餐：限定租户可用的菜单。平台专属功能（菜单、字典、租户、服务监控等）不能加入套餐
 */
@Service
@RequiredArgsConstructor
public class SysTenantPackageService extends ServiceImpl<SysTenantPackageMapper, SysTenantPackage> {

    private final SysTenantPackageMenuMapper packageMenuMapper;
    private final SysFeatureService featureService;
    private final SysTenantService tenantService;
    private final SysTenantLookup tenantLookup;

    public List<SysTenantPackage> list(String name) {
        List<SysTenantPackage> packages = list(Wrappers.<SysTenantPackage>lambdaQuery()
                .like(StringUtils.hasText(name), SysTenantPackage::getName, name)
                .orderByAsc(SysTenantPackage::getId));
        Map<Long, Long> counts = tenantService.countByPackages();
        packages.forEach(p -> p.setTenantCount(counts.getOrDefault(p.getId(), 0L)));
        return packages;
    }

    /** 启用的套餐，新建、修改租户时选择 */
    public List<SysTenantPackage> listEnabled() {
        return list(Wrappers.<SysTenantPackage>lambdaQuery()
                .select(SysTenantPackage::getId, SysTenantPackage::getName)
                .eq(SysTenantPackage::getStatus, 1)
                .orderByAsc(SysTenantPackage::getId));
    }

    public SysTenantPackage detail(Long id) {
        SysTenantPackage pkg = getExisting(id);
        pkg.setMenuIds(List.copyOf(tenantLookup.packageMenuIds(id)));
        return pkg;
    }

    /** 可加入套餐的菜单树（不含平台专属功能） */
    public List<SysMenu> menuTree() {
        return TreeUtils.build(assignableMenus());
    }

    @Transactional
    public void create(SysTenantPackage pkg) {
        checkNameUnique(pkg);
        List<Long> menuIds = checkMenus(pkg.getMenuIds());
        pkg.setId(null);
        if (pkg.getStatus() == null) pkg.setStatus(1);
        save(pkg);
        saveMenus(pkg.getId(), menuIds);
    }

    @Transactional
    public void update(SysTenantPackage pkg) {
        getExisting(pkg.getId());
        checkNameUnique(pkg);
        List<Long> menuIds = checkMenus(pkg.getMenuIds());
        updateById(pkg);
        saveMenus(pkg.getId(), menuIds);
        tenantService.syncPackageTenants(pkg.getId(), Set.copyOf(menuIds));
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            if (tenantService.countByPackage(id) > 0) {
                throw new BizException("套餐「" + getExisting(id).getName() + "」正在被租户使用，不能删除");
            }
        }
        removeByIds(ids);
        packageMenuMapper.delete(Wrappers.<SysTenantPackageMenu>lambdaQuery().in(SysTenantPackageMenu::getPackageId, ids));
    }

    private List<SysMenu> assignableMenus() {
        return featureService.assignableMenus();
    }

    /** 菜单必须存在且不是平台专属功能 */
    private List<Long> checkMenus(List<Long> menuIds) {
        List<Long> distinct = menuIds == null ? List.of() : menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            throw new BizException("请选择套餐包含的菜单");
        }
        Set<Long> assignable = assignableMenus().stream().map(SysMenu::getId).collect(Collectors.toSet());
        if (!assignable.containsAll(distinct)) {
            throw new BizException("菜单不存在或是平台专属功能，不能加入套餐");
        }
        return distinct;
    }

    private void saveMenus(Long packageId, List<Long> menuIds) {
        packageMenuMapper.delete(Wrappers.<SysTenantPackageMenu>lambdaQuery().eq(SysTenantPackageMenu::getPackageId, packageId));
        packageMenuMapper.insertBatch(packageId, menuIds);
    }

    private void checkNameUnique(SysTenantPackage pkg) {
        SysTenantPackage exist = getOne(Wrappers.<SysTenantPackage>lambdaQuery().eq(SysTenantPackage::getName, pkg.getName()));
        if (exist != null && !Objects.equals(exist.getId(), pkg.getId())) {
            throw new BizException("套餐名称已存在");
        }
    }

    private SysTenantPackage getExisting(Long id) {
        SysTenantPackage pkg = getById(id);
        if (pkg == null) {
            throw new BizException("套餐不存在");
        }
        return pkg;
    }
}
