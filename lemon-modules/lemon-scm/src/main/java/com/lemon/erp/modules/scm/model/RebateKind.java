package com.lemon.erp.modules.scm.model;

/**
 * 返利种类，与字典 scm_rebate_kind 对应：销售返利（给客户，结算生成红字应收或计入返利余额），
 * 采购返利（供应商给我方，结算生成红字应付，付款时与欠款冲抵）
 */
public enum RebateKind {
    SALES,
    PURCHASE
}
