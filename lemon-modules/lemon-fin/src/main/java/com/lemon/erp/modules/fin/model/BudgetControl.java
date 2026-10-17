package com.lemon.erp.modules.fin.model;

/**
 * 预算控制：费用报销超预算时的处理（字典 fin_budget_control）
 */
public enum BudgetControl {
    /** 不控制 */
    NONE,
    /** 超预算提示 */
    WARN,
    /** 超预算禁止 */
    BLOCK
}
