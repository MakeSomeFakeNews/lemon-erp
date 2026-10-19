package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信未绑定时用账号密码登录，成功后绑定当前微信
 *
 * @param key 登录返回的会话标识；其余字段同账号密码登录
 */
public record MiniappBindRequest(
        @NotBlank String key,
        @NotBlank(message = "{system.valid.usernameRequired}") String username,
        @NotBlank(message = "{system.valid.passwordRequired}") String password,
        String tenantName,
        String captchaId,
        String captchaCode) {

    public LoginRequest toLogin() {
        return new LoginRequest(username, password, tenantName, captchaId, captchaCode);
    }
}
