package com.lemon.erp.modules.ai.client;

import java.util.List;

/**
 * 模型的一次回答
 *
 * @param content      回答文字（只有工具调用时可能为空字符串）
 * @param toolCalls    请求调用的函数，没有时为空列表
 * @param finishReason 结束原因（stop、tool_calls、length 等，流式被取消时为 cancelled）
 * @param usage        token 用量
 * @param model        服务商返回的模型名称
 */
public record LlmResponse(String content, List<LlmToolCall> toolCalls, String finishReason, LlmUsage usage, String model) {

    public static final String FINISH_CANCELLED = "cancelled";

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
