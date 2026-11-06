package com.lemon.erp.modules.ai.dto;

import jakarta.validation.constraints.Size;

/** 新建 / 重命名会话 */
public record AiConversationRequest(@Size(max = 100, message = "{ai.valid.titleTooLong}") String title) {
}
