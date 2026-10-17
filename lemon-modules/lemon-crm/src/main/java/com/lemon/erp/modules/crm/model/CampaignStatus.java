package com.lemon.erp.modules.crm.model;

/**
 * 活动状态：市场活动的状态（字典 crm_campaign_status）
 */
public enum CampaignStatus {
    /** 计划中 */
    PLANNED,
    /** 进行中 */
    ACTIVE,
    /** 已结束 */
    FINISHED,
    /** 已取消 */
    CANCELED
}
