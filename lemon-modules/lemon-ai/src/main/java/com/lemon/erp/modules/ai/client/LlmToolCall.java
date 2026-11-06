package com.lemon.erp.modules.ai.client;

/**
 * 模型请求调用的函数
 *
 * @param id        调用 ID，回传结果时作为 tool_call_id
 * @param name      函数名称
 * @param arguments 参数（JSON 字符串，模型生成，需校验）
 */
public record LlmToolCall(String id, String name, String arguments) {
}
