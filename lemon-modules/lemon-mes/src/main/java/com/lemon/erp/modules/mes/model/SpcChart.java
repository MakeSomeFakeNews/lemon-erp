package com.lemon.erp.modules.mes.model;

/**
 * SPC 控制图类型：按子组大小选择
 */
public enum SpcChart {
    /** 单值-移动极差图（子组大小 1） */
    I_MR,
    /** 均值-极差图（子组大小 2 ~ 10） */
    XBAR_R,
    /** 均值-标准差图（子组大小大于 10） */
    XBAR_S
}
