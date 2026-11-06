package com.lemon.erp.modules.ai.dto;

/**
 * 当前用户生成报告时会包含的数据来源
 *
 * @param key     标识
 * @param label   名称（当前语言）
 * @param allData 是否全公司口径（false 表示按当前用户的数据权限统计）
 */
public record AiReportSourceView(String key, String label, boolean allData) {
}
