package com.lemon.erp.modules.scm.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 返利结算且兑付方式为红字应收（与结算同一事务）：财务生成红字应收单冲减应收，并回写返利协议的 billId
 *
 * @param amount 返利金额（正数，含税）
 */
public record RebateSettledEvent(Long rebateId, String rebateNo, Long customerId, BigDecimal amount, LocalDate settleDate) {
}
