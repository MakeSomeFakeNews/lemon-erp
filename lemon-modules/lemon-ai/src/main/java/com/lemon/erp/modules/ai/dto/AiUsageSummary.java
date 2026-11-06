package com.lemon.erp.modules.ai.dto;

import java.util.List;

/**
 * 本月用量（「AI 设置」页显示）
 *
 * @param month       月份 yyyy-MM
 * @param totalTokens 本月 token 合计（输入 + 输出）
 * @param limit       每月上限，为 null 不限
 * @param byFeature   按功能汇总
 * @param byUser      按调用人汇总（用量最多的前 20 人）
 */
public record AiUsageSummary(String month, long totalTokens, Long limit, List<AiUsageStat> byFeature, List<AiUsageStat> byUser) {
}
