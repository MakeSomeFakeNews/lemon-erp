package com.lemon.erp.modules.system.dto;

import com.lemon.erp.common.enums.EnableStatus;

/**
 * 用户列表查询条件
 *
 * @param username 用户名或昵称，模糊匹配
 * @param phone    手机号，模糊匹配
 * @param deptId   所属部门
 * @param includeChildren 按部门筛选时是否包含下级部门的用户，默认包含
 * @param internal 为 true 时只查内部用户，排除供应商 / 经销商门户账号（选择部门负责人、部门成员时使用）
 */
public record UserQuery(String username, String phone, EnableStatus status, Long deptId, Boolean includeChildren, Boolean internal) {

    public boolean includeChildDepts() {
        return !Boolean.FALSE.equals(includeChildren);
    }
}
