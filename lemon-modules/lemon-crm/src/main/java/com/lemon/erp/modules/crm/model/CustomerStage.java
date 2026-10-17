package com.lemon.erp.modules.crm.model;

/**
 * 客户阶段：客户生命周期阶段（字典 crm_customer_stage）
 */
public enum CustomerStage {
    /** 潜在客户 */
    POTENTIAL,
    /** 意向客户 */
    INTENT,
    /** 成交客户 */
    DEAL,
    /** 流失客户 */
    LOST
}
