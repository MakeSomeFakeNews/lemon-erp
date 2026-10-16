package com.lemon.erp.modules.mes.model;

/**
 * 不合格处置，与字典 qms_disposition 对应
 */
public enum Disposition {
    /** 退货（来料：保持冻结，办理采购退货） */
    RETURN,
    /** 返工（生成返工工单） */
    REWORK,
    /** 报废（来料：解冻并生成报废出库草稿） */
    SCRAP,
    /** 让步接收（来料：解冻放行） */
    CONCESSION,
    /** 挑选 */
    SORT
}
