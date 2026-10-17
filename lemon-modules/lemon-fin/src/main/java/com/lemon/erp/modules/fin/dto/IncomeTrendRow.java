package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;

/**
 * 利润表月度趋势：营业收入、营业成本、营业利润、净利润
 *
 * @param period yyyyMM
 */
public record IncomeTrendRow(int period, BigDecimal revenue, BigDecimal cost, BigDecimal operating, BigDecimal net) {
}
