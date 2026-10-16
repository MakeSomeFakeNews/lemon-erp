package com.lemon.erp.framework.security.datascope;

import java.util.Set;

/**
 * 登录用户的数据权限范围，由其全部启用角色的数据权限取并集得到，随 LoginUser 一起缓存
 *
 * @param all        能否访问全部数据（超级管理员，或任一角色为「全部数据」）
 * @param deptIds    可访问的部门 ID
 * @param selfUserId 可访问本人数据时为当前用户 ID，否则为 null
 */
public record DataScope(boolean all, Set<Long> deptIds, Long selfUserId) {

    public static final DataScope ALL = new DataScope(true, Set.of(), null);

    public DataScope {
        deptIds = deptIds == null ? Set.of() : Set.copyOf(deptIds);
    }

    public boolean canAccessDept(Long deptId) {
        return all || (deptId != null && deptIds.contains(deptId));
    }
}
