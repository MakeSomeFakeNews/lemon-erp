package com.lemon.erp.framework.security;

import org.springframework.stereotype.Component;

/**
 * 权限判断，配合 @PreAuthorize("@ss.hasPerm('sys:user:list')") 使用。
 * 超级管理员（角色编码 admin）拥有全部权限。
 */
@Component("ss")
public class PermissionService {

    public static final String ADMIN_ROLE = "admin";
    public static final String ALL_PERMISSION = "*:*:*";

    public boolean hasPerm(String perm) {
        LoginUser user = SecurityUtils.getLoginUser();
        return user.isAdmin() || user.perms().contains(ALL_PERMISSION) || user.perms().contains(perm);
    }

    /** 满足其一即可，如多个页面共用的选择器接口 */
    public boolean hasAnyPerm(String... perms) {
        for (String perm : perms) {
            if (hasPerm(perm)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String role) {
        LoginUser user = SecurityUtils.getLoginUser();
        return user.isAdmin() || user.roles().contains(role);
    }
}
