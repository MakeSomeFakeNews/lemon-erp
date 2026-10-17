package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.im.ImPlatform;

/**
 * 授权回调的结果
 *
 * @param bind            true 为个人中心绑定账号，false 为登录
 * @param token           登录令牌（登录时）
 * @param passwordExpired 需要修改密码（登录时）
 * @param redirect        登录后进入的页面
 */
public record ImCallbackResult(ImPlatform platform, boolean bind, String token, boolean passwordExpired, String redirect) {
}
