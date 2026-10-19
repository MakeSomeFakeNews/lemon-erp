package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 微信小程序登录结果：只绑定了一个账号时直接返回令牌；否则返回会话标识 key（10 分钟内有效），
 * tenants 为空表示未绑定（用账号密码登录并绑定），不为空时选择一个租户登录
 */
public record MiniappLoginResult(String token, boolean passwordExpired, String key, List<MiniappTenant> tenants) {

    public static MiniappLoginResult of(LoginResponse login) {
        return new MiniappLoginResult(login.token(), login.passwordExpired(), null, List.of());
    }

    /** @param name 租户名称，未开启 SaaS 模式时为空 */
    public record MiniappTenant(Long id, String name) {
    }
}
