package com.lemon.erp.modules.mes.model;

/**
 * 工单来源：工单是如何生成的
 */
public enum WorkOrderSource {
    /** 手工新建 */
    MANUAL,
    /** MRP 下达 */
    MRP,
    /** 生产计划转工单 */
    PLAN,
    /** 从其他工单拆分 */
    SPLIT,
    /** 返工 */
    REWORK,
    /** 齐套分析按缺料生成 */
    KIT
}
