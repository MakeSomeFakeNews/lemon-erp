package com.lemon.erp.modules.ai.dto;

/**
 * 一条建议的采纳结果
 *
 * @param id      建议 ID
 * @param success 是否成功
 * @param message 失败原因（按用户语言）
 */
public record AiCleanseApplyResult(Long id, boolean success, String message) {
}
