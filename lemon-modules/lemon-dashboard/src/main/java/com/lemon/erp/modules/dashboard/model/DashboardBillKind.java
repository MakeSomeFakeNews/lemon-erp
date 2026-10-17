package com.lemon.erp.modules.dashboard.model;

/**
 * 首页统计的往来单据种类，取值与 fin_bill.kind 一致（dashboard 不依赖 fin 模块，不能直接使用其枚举）
 */
public enum DashboardBillKind {
    /** 应收 */
    AR,
    /** 应付 */
    AP
}
