package com.lemon.erp.modules.ai.dto;

import tools.jackson.databind.JsonNode;

/**
 * 工具消息的结果数据（前端以表格显示）
 *
 * @param messageId 工具消息 ID
 * @param tool      工具名称
 * @param label     工具的显示名称
 * @param data      结果数据（列表或对象），失败时为 null
 * @param total     总行数（不知道时为 null）
 * @param truncated 是否截断
 * @param error     失败原因，成功时为 null
 */
public record AiToolData(Long messageId, String tool, String label, JsonNode data, Long total, boolean truncated, String error) {
}
