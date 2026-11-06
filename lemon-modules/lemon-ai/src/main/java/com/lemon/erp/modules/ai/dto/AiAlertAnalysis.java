package com.lemon.erp.modules.ai.dto;

import java.util.List;

/**
 * 经营预警的 AI 分析结果（不入库）
 *
 * @param ruleId       预警规则
 * @param content      分析（Markdown）
 * @param sourceLabels 参考的数据来源名称（当前用户有权限的相关来源）
 * @param tokens       消耗的 token
 */
public record AiAlertAnalysis(Long ruleId, String content, List<String> sourceLabels, int tokens) {
}
