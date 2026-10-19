package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param code          小程序 wx.login 返回的 code
 * @param switchAccount 使用其他账号登录：不按绑定自动登录，只返回会话标识（用于绑定另一个账号，如其他租户的账号）
 */
public record MiniappLoginRequest(@NotBlank String code, Boolean switchAccount) {
}
