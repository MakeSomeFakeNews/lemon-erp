package com.lemon.erp.framework.tenant;

import java.util.List;

/**
 * 只有平台租户可用的功能（按菜单权限前缀）：菜单、字典、参数设置、租户与套餐、服务与缓存监控、定时任务。
 * 这些菜单不能加入租户套餐，其他租户的用户即使被分配了也不生效
 */
public final class PlatformFeatures {

    private static final List<String> PERM_PREFIXES = List.of(
            "sys:menu:", "sys:dict:", "sys:config:", "sys:tenant", "monitor:server:", "monitor:cache:", "monitor:job:");

    private PlatformFeatures() {
    }

    public static boolean isPlatformOnly(String perms) {
        return perms != null && PERM_PREFIXES.stream().anyMatch(perms::startsWith);
    }

    /** 租户管理相关的功能，未开启 SaaS 模式时隐藏 */
    public static boolean isTenantManagement(String perms) {
        return perms != null && perms.startsWith("sys:tenant");
    }
}
