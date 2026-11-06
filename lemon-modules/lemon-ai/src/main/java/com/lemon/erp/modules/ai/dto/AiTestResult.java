package com.lemon.erp.modules.ai.dto;

/**
 * 测试连接的结果
 *
 * @param reply            模型的回复
 * @param model            服务商返回的模型名称
 * @param latencyMs        耗时（毫秒）
 * @param promptTokens     输入 token
 * @param completionTokens 输出 token
 */
public record AiTestResult(String reply, String model, long latencyMs, int promptTokens, int completionTokens) {
}
