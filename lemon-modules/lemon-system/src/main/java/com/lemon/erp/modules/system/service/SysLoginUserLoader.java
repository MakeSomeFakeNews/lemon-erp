package com.lemon.erp.modules.system.service;

import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.LoginUserLoader;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.tenant.PlatformFeatures;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.entity.SysRole;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysMenuMapper;
import com.lemon.erp.modules.system.mapper.SysRoleMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从数据库加载登录用户的角色、权限与数据权限，结果由 TokenService 缓存
 */
@Component
@RequiredArgsConstructor
public class SysLoginUserLoader implements LoginUserLoader {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysDataScopeResolver dataScopeResolver;
    private final SysTenantLookup tenantLookup;

    @Override
    public LoginUser loadLoginUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        Long tenantId = TenantContext.require();
        // 租户停用、过期或删除后，已登录的会话在权限缓存过期时随之失效
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !tenantLookup.isActive(tenantId)) {
            return null;
        }
        List<SysRole> roles = roleMapper.selectEnabledRolesByUserId(userId);
        Set<String> roleCodes = roles.stream().map(SysRole::getCode).collect(Collectors.toSet());
        boolean admin = roleCodes.contains(PermissionService.ADMIN_ROLE);
        Set<String> perms;
        if (admin && TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            perms = Set.of(PermissionService.ALL_PERMISSION);
        } else {
            // 其他租户的权限以套餐为上限，平台专属功能即使被分配也不生效
            boolean platform = TenantContext.PLATFORM_TENANT_ID.equals(tenantId);
            perms = menuMapper.selectPermsByUserId(userId, tenantLookup.currentPackageId()).stream()
                    .filter(p -> platform || !PlatformFeatures.isPlatformOnly(p))
                    .collect(Collectors.toCollection(HashSet::new));
        }
        PortalKind kind = user.getSupplierId() != null ? PortalKind.SUPPLIER : user.getCustomerId() != null ? PortalKind.DEALER : null;
        if (kind != null) {
            // 门户账号：不论分配了什么角色，只保留所属门户的权限，数据只到本人（门户接口按关联的供应商 / 客户过滤）
            Set<String> portalPerms = perms.stream().filter(kind::isPortalPerm).collect(Collectors.toSet());
            return new LoginUser(user.getId(), user.getUsername(), null, roleCodes, portalPerms,
                    new DataScope(false, Set.of(), user.getId()), null, tenantId, user.getSupplierId(), user.getCustomerId());
        }
        DataScope dataScope = admin ? DataScope.ALL : dataScopeResolver.resolve(user, roles);
        return new LoginUser(user.getId(), user.getUsername(), user.getDeptId(), roleCodes, perms, dataScope, null, tenantId, null, null);
    }
}
