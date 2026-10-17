package com.lemon.erp.modules.crm.model;

/**
 * 合同状态：销售合同的状态（字典 crm_contract_status）
 */
public enum SalesContractStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 执行中 */
    ACTIVE,
    /** 已完成 */
    COMPLETED,
    /** 已终止 */
    TERMINATED
}
