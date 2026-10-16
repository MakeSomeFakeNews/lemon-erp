package com.lemon.erp.modules.system.dto;

/**
 * 用户列表查询条件
 *
 * @param username 用户名或昵称，模糊匹配
 * @param phone    手机号，模糊匹配
 * @param deptId   所属部门
 * @param includeChildren 按部门筛选时是否包含下级部门的用户，默认包含
 */
public record UserQuery(String username, String phone, Integer status, Long deptId, Boolean includeChildren) {

    public boolean includeChildDepts() {
        return !Boolean.FALSE.equals(includeChildren);
    }
}
