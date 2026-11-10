package com.lemon.erp.framework.org;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 部门树查询，由系统模块实现（{@code SysDeptLookup}）。业务模块按「本部门及以下」汇总时使用（与数据权限的部门树处理一致），
 * 不直接访问 sys_dept 表
 */
public interface DeptLookup {

    /**
     * 多个部门各自及其全部下级部门的 ID（只查询一次部门表）
     *
     * @return 部门 ID → 自身及全部下级部门的 ID
     */
    Map<Long, Set<Long>> selfAndDescendantIds(Collection<Long> deptIds);
}
