package com.lemon.erp.modules.mes.model;

/**
 * 生产计划状态，与字典 mes_plan_status 对应：草稿可修改；确认后参与 MRP 运算；关闭后不再参与
 */
public enum PlanStatus {
    DRAFT,
    CONFIRMED,
    CLOSED
}
