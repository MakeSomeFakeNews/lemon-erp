package com.lemon.erp.modules.fin.model;

/**
 * 销售提成依据（字典 fin_commission_basis）
 */
public enum CommissionBasis {
    /** 销售额：销售出库的不含税金额，扣除退货 */
    SALES,
    /** 毛利：销售额 - 出库数量 × 当期成本单价（未核算时取参考成本价） */
    PROFIT,
    /** 回款：收款核销的销售出库 / 退货应收单金额 */
    RECEIPT
}
