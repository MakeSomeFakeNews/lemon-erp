package com.lemon.erp.framework.security;

import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.tenant.TenantContext;

import java.util.Set;

/**
 * 当前登录用户，放在 SecurityContext 的 principal 中
 *
 * @param deptId    所属部门，可能为 null
 * @param dataScope 数据权限范围
 * @param tokenId   当前请求所用 token 的会话 ID（jti），缓存中的对象该字段为 null
 * @param tenantId  所属租户，未开启 SaaS 模式时为平台租户
 * @param supplierId 供应商门户账号关联的供应商，内部用户为 null
 * @param customerId 经销商门户账号关联的客户，内部用户为 null
 */
public record LoginUser(Long userId, String username, Long deptId, Set<String> roles, Set<String> perms,
                        DataScope dataScope, String tokenId, Long tenantId, Long supplierId, Long customerId) {

    /**
     * 超级管理员：平台租户中角色编码为 admin 的用户，拥有全部权限。
     * 其他租户的 admin 角色是租户管理员，权限以套餐为上限，按普通角色处理
     */
    public boolean isAdmin() {
        return roles.contains(PermissionService.ADMIN_ROLE) && TenantContext.PLATFORM_TENANT_ID.equals(tenantId);
    }

    public LoginUser withTokenId(String tokenId) {
        return new LoginUser(userId, username, deptId, roles, perms, dataScope, tokenId, tenantId, supplierId, customerId);
    }

    /** 供应商门户账号：只能访问门户功能（portal:* 权限），数据限定为关联的供应商 */
    public boolean isSupplier() {
        return supplierId != null;
    }

    /** 经销商门户账号：只能访问经销商门户（dealer:* 权限），数据限定为关联的客户 */
    public boolean isDealer() {
        return customerId != null;
    }

    /** 门户账号所属的门户，内部用户为 null */
    public PortalKind portalKind() {
        return isSupplier() ? PortalKind.SUPPLIER : isDealer() ? PortalKind.DEALER : null;
    }
}
