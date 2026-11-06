package com.lemon.erp.modules.ai.client;

import tools.jackson.databind.JsonNode;

/**
 * JSON 输出的结果（已按 schema 校验）
 *
 * @param value    模型输出的 JSON 对象
 * @param usage    全部请求（含校验失败后的重试）的用量合计
 * @param attempts 请求次数（1 或 2）
 */
public record LlmJsonResult(JsonNode value, LlmUsage usage, int attempts) {
}
