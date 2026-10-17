package com.lemon.erp.modules.system.dto;

import com.lemon.erp.modules.system.im.ImPlatform;

/** 登录页显示的第三方登录方式 */
public record ImProvider(ImPlatform platform, String label) {
}
