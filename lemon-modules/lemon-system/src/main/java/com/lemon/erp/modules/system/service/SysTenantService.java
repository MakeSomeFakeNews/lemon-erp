package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.security.datascope.DataScopeType;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.system.dto.TenantAccountCount;
import com.lemon.erp.modules.system.entity.SysDept;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysRoleMenu;
import com.lemon.erp.modules.system.entity.SysTenant;
import com.lemon.erp.modules.system.entity.SysTenantPackage;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysDeptMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMenuMapper;
import com.lemon.erp.modules.system.mapper.SysTenantMapper;
import com.lemon.erp.modules.system.mapper.SysTenantPackageMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户管理（SaaS 模式）。租户 1 是平台租户，不受套餐限制、不能停用删除；
 * 新建租户时开通根部门、租户管理员（角色编码 admin，菜单随套餐同步），复制平台的内置岗位角色，
 * 再由各模块的 {@link TenantInitializer} 初始化基础数据
 */
@Service
@RequiredArgsConstructor
public class SysTenantService extends ServiceImpl<SysTenantMapper, SysTenant> {

    /** 新租户从平台复制的内置岗位角色（菜单取与套餐的交集） */
    static final List<String> BUILT_IN_ROLES = List.of(
            "common", "gm", "purchaser", "sales", "warehouse", "planner", "workshop", "quality", "accountant", "cashier", "hr", "pm", "staff", "supplier",
            "dealer");

    private final SysTenantLookup lookup;
    private final SysTenantPackageMapper packageMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final ObjectProvider<TenantInitializer> initializers;

    public PageResult<SysTenant> page(long current, long size, String name, Integer status) {
        requireEnabled();
        Page<SysTenant> page = page(Page.of(current, size), Wrappers.<SysTenant>lambdaQuery()
                .like(StringUtils.hasText(name), SysTenant::getName, name)
                .eq(status != null, SysTenant::getStatus, status)
                .orderByAsc(SysTenant::getId));
        fill(page.getRecords());
        return PageResult.of(page);
    }

    public SysTenant detail(Long id) {
        requireEnabled();
        SysTenant tenant = getExisting(id);
        fill(List.of(tenant));
        if (tenant.getAdminUserId() != null) {
            SysUser admin = TenantContext.callAs(id, () -> userMapper.selectById(tenant.getAdminUserId()));
            tenant.setAdminUsername(admin == null ? null : admin.getUsername());
        }
        return tenant;
    }

    @Transactional
    public void create(SysTenant tenant) {
        requireEnabled();
        checkNameUnique(tenant);
        Set<Long> menuIds = lookup.packageMenuIds(checkPackage(tenant.getPackageId()).getId());
        passwordPolicy.check(tenant.getAdminPassword());
        tenant.setId(null);
        tenant.setAdminUserId(null);
        if (tenant.getStatus() == null) tenant.setStatus(1);
        save(tenant);
        TenantContext.runAs(tenant.getId(), () -> {
            Map<Long, Long> roleIdMap = new HashMap<>();
            Long adminUserId = initOrganization(tenant, menuIds, roleIdMap);
            update(Wrappers.<SysTenant>lambdaUpdate().set(SysTenant::getAdminUserId, adminUserId)
                    .eq(SysTenant::getId, tenant.getId()));
            TenantInitializer.Context context = new TenantInitializer.Context(tenant.getId(), tenant.getName(),
                    Set.copyOf(menuIds), Map.copyOf(roleIdMap));
            initializers.orderedStream().forEach(i -> i.initialize(context));
        });
    }

    @Transactional
    public void update(SysTenant tenant) {
        requireEnabled();
        SysTenant old = getExisting(tenant.getId());
        checkNameUnique(tenant);
        if (old.isPlatform()) {
            // 平台租户不受套餐、到期时间、账号数限制，也不能停用
            tenant.setPackageId(null);
            tenant.setExpireTime(null);
            tenant.setAccountLimit(null);
            tenant.setStatus(1);
        } else {
            checkPackage(tenant.getPackageId());
        }
        tenant.setAdminUserId(null);
        updateById(tenant);
        if (!old.isPlatform() && !Objects.equals(old.getPackageId(), tenant.getPackageId())) {
            syncMenus(tenant.getId(), lookup.packageMenuIds(tenant.getPackageId()));
            tokenService.evictAllLoginUsers();
        }
        if (Integer.valueOf(0).equals(tenant.getStatus()) && !Integer.valueOf(0).equals(old.getStatus())) {
            tokenService.kickTenant(tenant.getId());
        }
    }

    public void changeStatus(Long id, Integer status) {
        requireEnabled();
        SysTenant tenant = getExisting(id);
        if (tenant.isPlatform() && Integer.valueOf(0).equals(status)) {
            throw new BizException("不允许停用平台租户");
        }
        update(Wrappers.<SysTenant>lambdaUpdate().set(SysTenant::getStatus, status)
                .set(SysTenant::getUpdateTime, LocalDateTime.now()).eq(SysTenant::getId, id));
        if (Integer.valueOf(0).equals(status)) {
            tokenService.kickTenant(id);
        }
    }

    /** 只删除租户记录（之后无法登录），业务数据保留，便于审计或恢复 */
    @Transactional
    public void delete(List<Long> ids) {
        requireEnabled();
        if (ids.contains(TenantContext.PLATFORM_TENANT_ID)) {
            throw new BizException("不允许删除平台租户");
        }
        List<SysTenant> tenants = listByIds(ids);
        if (tenants.stream().anyMatch(t -> !Integer.valueOf(0).equals(t.getStatus()))) {
            throw new BizException("请先停用租户再删除");
        }
        removeByIds(ids);
        ids.forEach(tokenService::kickTenant);
    }

    /**
     * 套餐菜单变化后同步使用该套餐的租户：租户管理员的菜单与套餐一致，其他角色去掉套餐之外的菜单
     */
    void syncPackageTenants(Long packageId, Set<Long> menuIds) {
        list(Wrappers.<SysTenant>lambdaQuery().select(SysTenant::getId).eq(SysTenant::getPackageId, packageId))
                .forEach(t -> syncMenus(t.getId(), menuIds));
        tokenService.evictAllLoginUsers();
    }

    long countByPackage(Long packageId) {
        return count(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getPackageId, packageId));
    }

    Map<Long, Long> countByPackages() {
        return list(Wrappers.<SysTenant>lambdaQuery().select(SysTenant::getPackageId).isNotNull(SysTenant::getPackageId))
                .stream().collect(Collectors.groupingBy(SysTenant::getPackageId, Collectors.counting()));
    }

    private void syncMenus(Long tenantId, Set<Long> menuIds) {
        TenantContext.runAs(tenantId, () -> {
            List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().select(SysRole::getId, SysRole::getCode));
            if (roles.isEmpty()) {
                return;
            }
            roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery()
                    .in(SysRoleMenu::getRoleId, roles.stream().map(SysRole::getId).toList())
                    .notIn(!menuIds.isEmpty(), SysRoleMenu::getMenuId, menuIds));
            roles.stream().filter(r -> PermissionService.ADMIN_ROLE.equals(r.getCode())).forEach(r -> {
                roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, r.getId()));
                if (!menuIds.isEmpty()) {
                    roleMenuMapper.insertBatch(r.getId(), menuIds);
                }
            });
        });
    }

    /** 新租户的根部门、租户管理员角色和账号、内置岗位角色；返回管理员用户 ID */
    private Long initOrganization(SysTenant tenant, Set<Long> menuIds, Map<Long, Long> roleIdMap) {
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setName(tenant.getName());
        dept.setSort(1);
        dept.setStatus(1);
        deptMapper.insert(dept);

        SysRole adminRole = new SysRole();
        adminRole.setName("租户管理员");
        adminRole.setCode(PermissionService.ADMIN_ROLE);
        adminRole.setSort(1);
        adminRole.setDataScope(DataScopeType.ALL.code());
        adminRole.setStatus(1);
        adminRole.setRemark("拥有套餐内的全部权限，菜单随套餐自动调整");
        roleMapper.insert(adminRole);
        if (!menuIds.isEmpty()) {
            roleMenuMapper.insertBatch(adminRole.getId(), menuIds);
        }

        SysUser admin = new SysUser();
        admin.setDeptId(dept.getId());
        admin.setUsername(tenant.getAdminUsername().trim());
        admin.setNickname("管理员");
        admin.setPassword(passwordEncoder.encode(tenant.getAdminPassword()));
        admin.setPwdUpdateTime(LocalDateTime.now());
        admin.setPwdReset(1);
        admin.setStatus(1);
        admin.setPhone(tenant.getContactPhone());
        userMapper.insert(admin);
        userRoleMapper.insertBatch(admin.getId(), List.of(adminRole.getId()));

        dept.setLeaderUserId(admin.getId());
        deptMapper.updateById(dept);

        copyBuiltInRoles(menuIds, roleIdMap);
        return admin.getId();
    }

    private void copyBuiltInRoles(Set<Long> menuIds, Map<Long, Long> roleIdMap) {
        record Template(SysRole role, List<Long> menuIds) {
        }
        List<Template> templates = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> {
            List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                    .in(SysRole::getCode, BUILT_IN_ROLES).orderByAsc(SysRole::getSort));
            Map<Long, List<Long>> menus = roles.isEmpty() ? Map.of() : roleMenuMapper.selectList(
                            Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roles.stream().map(SysRole::getId).toList()))
                    .stream().collect(Collectors.groupingBy(SysRoleMenu::getRoleId,
                            Collectors.mapping(SysRoleMenu::getMenuId, Collectors.toList())));
            return roles.stream().map(r -> new Template(r, menus.getOrDefault(r.getId(), List.of()))).toList();
        });
        for (Template t : templates) {
            Long platformRoleId = t.role().getId();
            SysRole role = TenantInitializer.asNew(t.role());
            // 自定义数据权限关联的是平台的部门，新租户改为本部门
            if (Objects.equals(role.getDataScope(), DataScopeType.CUSTOM.code())) {
                role.setDataScope(DataScopeType.DEPT.code());
            }
            roleMapper.insert(role);
            roleIdMap.put(platformRoleId, role.getId());
            List<Long> granted = t.menuIds().stream().filter(menuIds::contains).toList();
            if (!granted.isEmpty()) {
                roleMenuMapper.insertBatch(role.getId(), granted);
            }
        }
    }

    private void fill(List<SysTenant> tenants) {
        if (tenants.isEmpty()) {
            return;
        }
        Set<Long> packageIds = tenants.stream().map(SysTenant::getPackageId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> packageNames = packageIds.isEmpty() ? Map.of() : packageMapper.selectByIds(packageIds).stream()
                .collect(Collectors.toMap(SysTenantPackage::getId, SysTenantPackage::getName));
        Map<Long, Long> counts = baseMapper.selectAccountCounts(tenants.stream().map(SysTenant::getId).toList()).stream()
                .collect(Collectors.toMap(TenantAccountCount::tenantId, TenantAccountCount::accountCount));
        tenants.forEach(t -> {
            t.setPackageName(t.getPackageId() == null ? null : packageNames.get(t.getPackageId()));
            t.setAccountCount(counts.getOrDefault(t.getId(), 0L));
        });
    }

    private SysTenantPackage checkPackage(Long packageId) {
        if (packageId == null) {
            throw new BizException("请选择套餐");
        }
        SysTenantPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw new BizException("套餐不存在");
        }
        if (!Integer.valueOf(1).equals(pkg.getStatus())) {
            throw new BizException("套餐「" + pkg.getName() + "」已停用");
        }
        return pkg;
    }

    private void checkNameUnique(SysTenant tenant) {
        tenant.setName(tenant.getName().trim());
        SysTenant exist = getOne(Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getName, tenant.getName()));
        if (exist != null && !Objects.equals(exist.getId(), tenant.getId())) {
            throw new BizException("租户名称已存在");
        }
    }

    private SysTenant getExisting(Long id) {
        SysTenant tenant = getById(id);
        if (tenant == null) {
            throw new BizException("租户不存在");
        }
        return tenant;
    }

    private void requireEnabled() {
        if (!lookup.isEnabled()) {
            throw new BizException("未开启 SaaS 模式（lemon.tenant.enabled）");
        }
    }
}
