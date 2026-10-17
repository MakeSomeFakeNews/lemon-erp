package com.lemon.erp.modules.pm.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 成本类型：项目成本的类型（字典 pm_cost_type）
 */
public enum CostType {
    /** 人工 */
    LABOR,
    /** 材料 */
    MATERIAL,
    /** 差旅 */
    TRAVEL,
    /** 外包 */
    OUTSOURCE,
    /** 设备 */
    EQUIPMENT,
    /** 其他 */
    OTHER;

    /** 可以手工录入费用的类型（人工成本来自已审批的工时） */
    public static final Set<CostType> MANUAL = EnumSet.complementOf(EnumSet.of(LABOR));

    /** 是否可以手工录入费用 */
    public boolean isManual() {
        return MANUAL.contains(this);
    }
}
