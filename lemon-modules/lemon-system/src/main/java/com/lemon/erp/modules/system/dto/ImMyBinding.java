package com.lemon.erp.modules.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.system.im.ImPlatform;

import java.time.LocalDateTime;

/**
 * 个人中心的账号绑定：当前租户已启用的平台及本人的绑定情况
 *
 * @param imName 绑定的成员姓名（未绑定为空）
 */
public record ImMyBinding(ImPlatform platform, String label, boolean bound, String imUserId, String imName,
                          @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime bindTime) {
}
