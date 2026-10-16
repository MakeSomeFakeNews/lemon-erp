package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;

/**
 * 报表行（资产负债表、利润表）
 *
 * @param level  缩进层级：0 合计 / 小计行，1 明细行
 * @param amount 期末余额 / 本期金额
 * @param amount2 年初余额 / 本年累计
 */
public record ReportLine(String name, int level, BigDecimal amount, BigDecimal amount2, boolean bold) {
}
