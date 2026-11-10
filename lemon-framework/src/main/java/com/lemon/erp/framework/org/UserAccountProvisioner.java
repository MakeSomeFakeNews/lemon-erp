package com.lemon.erp.framework.org;

/**
 * 为新员工开通登录账号，由系统模块实现（{@code SysOrgSyncService}）。初始密码取系统参数「初始密码」，首次登录要求修改；
 * 授予内置的「普通员工」角色（员工自助、发起审批），其他角色由管理员在用户管理中分配
 */
public interface UserAccountProvisioner {

    /**
     * @param username 登录名，已存在时报错
     * @return 新账号 ID
     */
    Long create(String username, String nickname, Long deptId, String phone, String email);
}
