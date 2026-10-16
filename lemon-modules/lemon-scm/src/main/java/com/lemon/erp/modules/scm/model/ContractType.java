package com.lemon.erp.modules.scm.model;

/**
 * 采购合同类型，与字典 scm_contract_type 对应
 */
public enum ContractType {
    /** 框架协议：约定价格和期限，数量按订单执行，可设金额上限 */
    FRAMEWORK,
    /** 固定数量合同：明细有数量上限 */
    FIXED
}
