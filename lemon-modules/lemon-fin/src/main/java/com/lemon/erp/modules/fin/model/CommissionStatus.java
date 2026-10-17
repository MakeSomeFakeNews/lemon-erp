package com.lemon.erp.modules.fin.model;

/**
 * 销售提成表状态：草稿可以重新计算、调整，确认后计入当月工资表
 */
public enum CommissionStatus {
    DRAFT,
    CONFIRMED
}
