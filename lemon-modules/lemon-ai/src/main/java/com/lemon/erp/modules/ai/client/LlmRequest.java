package com.lemon.erp.modules.ai.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一次对话请求
 *
 * @param messages    消息（系统提示、历史、本次提问、工具结果）
 * @param tools       提供给模型的函数，没有时为空列表
 * @param jsonObject  要求输出 JSON 对象（response_format = json_object）
 * @param temperature 本次的温度，为 null 时用 endpoint 的默认值
 * @param maxTokens   本次的最大输出 token 数，为 null 时用 endpoint 的默认值
 */
public record LlmRequest(List<LlmMessage> messages, List<LlmTool> tools, boolean jsonObject, BigDecimal temperature, Integer maxTokens) {

    public LlmRequest {
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }

    public static LlmRequest of(List<LlmMessage> messages) {
        return new LlmRequest(messages, null, false, null, null);
    }

    public static LlmRequest of(LlmMessage... messages) {
        return of(List.of(messages));
    }

    /** 带工具的对话 */
    public static LlmRequest withTools(List<LlmMessage> messages, List<LlmTool> tools) {
        return new LlmRequest(messages, tools, false, null, null);
    }

    public LlmRequest withMessages(List<LlmMessage> newMessages) {
        return new LlmRequest(newMessages, tools, jsonObject, temperature, maxTokens);
    }

    public LlmRequest withJsonObject() {
        return new LlmRequest(messages, tools, true, temperature, maxTokens);
    }

    public LlmRequest withTemperature(BigDecimal value) {
        return new LlmRequest(messages, tools, jsonObject, value, maxTokens);
    }

    public LlmRequest withMaxTokens(Integer value) {
        return new LlmRequest(messages, tools, jsonObject, temperature, value);
    }

    /** 是否有图片（需要识图模型） */
    public boolean hasImages() {
        return messages.stream().anyMatch(LlmMessage::hasImages);
    }
}
