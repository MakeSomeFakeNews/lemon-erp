package com.lemon.erp.modules.scm.model;

/**
 * 运费付给承运商的方式（销售出库、采购退货），与字典 scm_freight_settle 对应
 */
public enum FreightSettle {
    /** 现付：选择付款账户，财务生成费用支出单（来源为该出入库单），出纳确认后生成凭证 */
    CASH,
    /** 月结：生成承运商（物流公司）的应付单，以后付款核销 */
    MONTHLY
}
