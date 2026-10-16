package com.lemon.erp.modules.system.dto;

/**
 * @param passwordExpired 密码由管理员设置（初始密码、重置）或已超过有效期，前端提示修改
 */
public record LoginResponse(String token, boolean passwordExpired) {
}
