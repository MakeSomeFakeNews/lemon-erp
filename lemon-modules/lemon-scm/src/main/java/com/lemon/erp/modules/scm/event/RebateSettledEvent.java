package com.lemon.erp.modules.scm.event;

import com.lemon.erp.modules.scm.model.RebateKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 返利结算且兑付方式为红字单据（与结算同一事务）：财务生成红字应收单（销售返利，冲减应收）或红字应付单（采购返利，冲减应付），
 * 并回写返利协议的 billId
 *
 * @param partnerId 销售返利为客户，采购返利为供应商
 * @param amount    返利金额（正数，含税）
 */
public record RebateSettledEvent(Long rebateId, String rebateNo, RebateKind kind, Long partnerId, BigDecimal amount, LocalDate settleDate) {
}
