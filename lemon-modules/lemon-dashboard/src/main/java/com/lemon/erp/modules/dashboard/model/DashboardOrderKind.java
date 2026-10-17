package com.lemon.erp.modules.dashboard.model;

/**
 * 首页统计的订单种类，取值与 scm_order.kind 一致（dashboard 不依赖 scm 模块，不能直接使用其枚举）
 */
public enum DashboardOrderKind {
    /** 销售订单 */
    SALES,
    /** 采购订单 */
    PURCHASE
}
