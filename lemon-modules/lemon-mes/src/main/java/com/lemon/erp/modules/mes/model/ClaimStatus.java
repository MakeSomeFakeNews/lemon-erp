package com.lemon.erp.modules.mes.model;

/**
 * 供应商索赔状态，与字典 qms_claim_status 对应
 */
public enum ClaimStatus {
    /** 待确认 */
    DRAFT,
    /** 已确认（已冲减应付） */
    CONFIRMED,
    /** 已取消 */
    CANCELLED
}
