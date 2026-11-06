package com.lemon.erp.modules.ai.dto;

/**
 * 用量汇总的一行
 *
 * @param key              功能（AiFeature 名称）或调用人 ID
 * @param name             调用人姓名（按功能汇总时为 null）
 * @param calls            调用次数
 * @param promptTokens     输入 token
 * @param completionTokens 输出 token
 */
public record AiUsageStat(String key, String name, long calls, long promptTokens, long completionTokens) {
}
