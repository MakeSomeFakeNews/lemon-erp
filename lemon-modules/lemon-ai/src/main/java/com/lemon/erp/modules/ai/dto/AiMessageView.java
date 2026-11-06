package com.lemon.erp.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ai.model.AiMessageRole;

import java.time.LocalDateTime;

/**
 * 消息历史中的一条（不含工具调用请求与工具结果的大字段，工具结果用 /ai/chat/messages/{id}/data 按需读取）
 *
 * @param id           消息 ID
 * @param role         角色
 * @param content      文字；工具消息为调用参数（JSON）
 * @param toolName     工具消息：工具名称
 * @param toolLabel    工具消息：工具的显示名称（当前语言）
 * @param hasToolCalls 模型消息：是否请求了工具调用（中间步骤）
 * @param tokens       模型消息：消耗的 token
 * @param createTime   时间
 */
public record AiMessageView(Long id, AiMessageRole role, String content, String toolName, String toolLabel,
                            boolean hasToolCalls, Integer tokens,
                            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) {
}
