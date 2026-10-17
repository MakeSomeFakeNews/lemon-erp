package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报销付款、借款付款 / 还款：资金账户、日期，还款时填金额
 */
public record EmployeePayRequest(Long accountId, LocalDate date, BigDecimal amount) {
}
