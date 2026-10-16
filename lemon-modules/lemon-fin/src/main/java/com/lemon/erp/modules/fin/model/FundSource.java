package com.lemon.erp.modules.fin.model;

/**
 * 资金计划中预计收支的来源
 */
public enum FundSource {
    /** 未核销的应收单 */
    AR,
    /** 未核销的应付单 */
    AP,
    /** 持有中的应收票据到期 */
    NOTE_IN,
    /** 未兑付的应付票据到期 */
    NOTE_OUT,
    /** 已审批待付款的报销 */
    CLAIM,
    /** 手工计划项 */
    PLAN
}
