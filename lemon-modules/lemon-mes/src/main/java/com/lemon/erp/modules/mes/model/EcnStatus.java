package com.lemon.erp.modules.mes.model;

/**
 * 工程变更状态，与字典 mes_ecn_status 对应
 */
public enum EcnStatus {
    DRAFT,
    APPROVING,
    /** 已批准，等待生效日期 */
    APPROVED,
    EFFECTIVE,
    CANCELLED
}
