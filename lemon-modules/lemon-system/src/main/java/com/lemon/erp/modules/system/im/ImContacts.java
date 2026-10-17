package com.lemon.erp.modules.system.im;

import java.util.List;

/**
 * 平台通讯录：部门按上级在前的顺序排列，根部门（企业本身）的 parentId 为空
 */
public record ImContacts(List<Dept> depts, List<Member> members) {

    public record Dept(String id, String parentId, String name, int sort) {
    }

    /**
     * @param mobile 手机号，平台未开放手机号权限时为空
     * @param deptId 所属的第一个部门
     */
    public record Member(String userId, String name, String mobile, String email, String deptId) {
    }
}
