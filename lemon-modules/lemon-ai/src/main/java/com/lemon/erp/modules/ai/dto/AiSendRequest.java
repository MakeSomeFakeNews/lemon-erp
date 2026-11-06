package com.lemon.erp.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发送一条提问 */
public record AiSendRequest(@NotBlank(message = "{ai.valid.contentRequired}") @Size(max = 4000, message = "{ai.valid.contentTooLong}") String content) {
}
