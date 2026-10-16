package com.lemon.erp.modules.scm.model;

/**
 * 采购合同状态，与字典 scm_contract_status 对应
 */
public enum ContractStatus {
    DRAFT,
    APPROVING,
    /** 生效：期限内采购订单可以引用 */
    ACTIVE,
    CLOSED
}
