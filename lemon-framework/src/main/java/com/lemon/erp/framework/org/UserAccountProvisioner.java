package com.lemon.erp.framework.org;

/**
 * 为新员工开通登录账号，由系统模块实现（{@code SysUserAccountProvisioner}）。初始密码与「重置密码」默认值一致
 */
public interface UserAccountProvisioner {

    /**
     * @param username 登录名，已存在时报错
     * @return 新账号 ID
     */
    Long create(String username, String nickname, Long deptId, String phone, String email);
}
