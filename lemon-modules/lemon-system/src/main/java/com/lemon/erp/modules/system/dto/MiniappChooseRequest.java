package com.lemon.erp.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 微信绑定了多个租户的账号时选择其中一个登录
 *
 * @param key 登录返回的会话标识
 */
public record MiniappChooseRequest(@NotBlank String key, @NotNull Long tenantId) {
}
