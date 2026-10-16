package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param tenantName  租户名称，SaaS 模式下必填，未开启时忽略
 * @param captchaId   验证码 ID（/auth/captcha 返回），需要验证码时必填
 * @param captchaCode 验证码
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        String tenantName,
        String captchaId,
        String captchaCode) {
}
