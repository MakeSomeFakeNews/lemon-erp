package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.im.ImPlatform;

/**
 * @param bound   true 只看已绑定，false 只看未绑定，为空全部
 * @param keyword 用户名、姓名、手机号或成员 userid
 */
public record ImBindingQuery(ImPlatform platform, Boolean bound, String keyword) {
}
