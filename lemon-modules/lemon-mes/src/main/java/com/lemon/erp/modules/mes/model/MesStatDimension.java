package com.lemon.erp.modules.mes.model;

/**
 * 生产统计报表的汇总维度，各报表可用的维度见 {@code MesStatService}
 */
public enum MesStatDimension {
    /** 人员（报工的操作员） */
    OPERATOR,
    /** 人员 × 工序（工作中心 + 工序名称） */
    OPERATOR_OP,
    /** 人员 × 日期 */
    OPERATOR_DAY,
    /** 工作中心 */
    WORK_CENTER,
    /** 工序：工作中心 + 工序名称 */
    OPERATION,
    /** 产品 */
    PRODUCT,
    /** 产品分类（业务字典 wms_material_category） */
    CATEGORY,
    /** 车间：工单的部门 */
    DEPT,
    /** 日 */
    DAY,
    /** 月 */
    MONTH,
    /** 工单（不汇总） */
    WORK_ORDER,
    /** 物料（用料） */
    MATERIAL,
    /** 工单 × 物料 */
    WO_MATERIAL;

    public boolean isDate() {
        return this == DAY || this == MONTH || this == OPERATOR_DAY;
    }
}
