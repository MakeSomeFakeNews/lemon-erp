package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;

/**
 * 财务指标
 *
 * @param value  资产负债表为期末、利润表为本期
 * @param value2 资产负债表为年初、利润表为本年累计
 * @param unit   % 或 倍
 * @param hint   计算公式
 */
public record FinancialMetric(String name, BigDecimal value, BigDecimal value2, String unit, String hint) {
}
