package com.lemon.erp.modules.scm.model;

/**
 * 返利兑付方式，与字典 scm_rebate_settle 对应：红字应收冲减应收，或作为余额在下次销售订单中抵扣
 */
public enum RebateSettle {
    CREDIT_NOTE,
    NEXT_ORDER
}
