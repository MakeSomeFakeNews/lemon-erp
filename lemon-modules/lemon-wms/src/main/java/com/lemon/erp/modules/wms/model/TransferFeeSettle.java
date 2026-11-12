package com.lemon.erp.modules.wms.model;

/**
 * 调拨费用（配送费）付给承运商的方式，取值与字典 scm_freight_settle（销售出库运费的结算方式）相同
 */
public enum TransferFeeSettle {
    /** 现付：选择付款账户，财务生成费用支出单（来源为调拨单），出纳确认后生成凭证 */
    CASH,
    /** 月结：生成承运商（物流公司）的应付单，以后付款核销 */
    MONTHLY
}
