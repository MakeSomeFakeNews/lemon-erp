package com.lemon.erp.framework.portal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 外部门户：供应商门户（/portal，portal:*）、经销商门户（/dealer，dealer:*）。
 * 门户账号是关联了供应商 / 客户的用户，只分配内置的门户角色，登录后只显示和只能访问对应的门户
 */
@Getter
@AllArgsConstructor
public enum PortalKind {
    SUPPLIER("supplier", "/portal", "portal:", "framework.enum.PortalKind.supplier"),
    DEALER("dealer", "/dealer", "dealer:", "framework.enum.PortalKind.dealer");

    /** 内置角色编码 */
    private final String role;
    /** 门户模块的路由地址（顶部导航一级目录），也是门户接口的路径前缀 */
    private final String modulePath;
    /** 门户权限标识前缀 */
    private final String permPrefix;
    /** 名称的消息键 */
    private final String label;

    /** 门户账号还可以访问的公共接口：登录信息、个人中心、站内消息、字典、附件（附件按业务记录再校验） */
    private static final List<String> COMMON_PATHS = List.of("/auth/", "/profile", "/system/message", "/system/dict/data/type/",
            "/system/attachment");

    /** 门户账号只能访问本门户和公共接口（不依赖各接口是否声明了权限） */
    public boolean allows(String path) {
        return path.startsWith(modulePath + "/") || COMMON_PATHS.stream().anyMatch(path::startsWith);
    }

    public boolean isPortalPerm(String perm) {
        return perm != null && perm.startsWith(permPrefix);
    }

    public static PortalKind ofModulePath(String path) {
        for (PortalKind k : values()) {
            if (k.modulePath.equals(path)) {
                return k;
            }
        }
        return null;
    }
}
