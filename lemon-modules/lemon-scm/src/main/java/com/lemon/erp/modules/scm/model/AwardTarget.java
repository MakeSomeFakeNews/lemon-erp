package com.lemon.erp.modules.scm.model;

/**
 * 定标后生成的单据
 */
public enum AwardTarget {
    /** 采购合同（草稿），明细数量为招标数量 */
    CONTRACT,
    /** 采购订单（草稿） */
    ORDER,
    /** 只定标，不生成单据 */
    NONE
}
