package com.lemon.erp.modules.ai.client;

import java.util.Map;

/**
 * 提供给模型的函数（OpenAI tools 中 type 为 function 的一项）
 *
 * @param name        函数名称
 * @param description 给模型看的说明
 * @param parameters  参数的 JSON Schema
 */
public record LlmTool(String name, String description, Map<String, Object> parameters) {
}
