package com.lemon.erp.modules.fin.model;

/**
 * 成本核算记录种类
 */
public enum CostRunKind {
    /** 月度成本核算 */
    RUN,
    /** 存货期初：启用期间的上一期，保存期初数量、金额作为首期核算的期初 */
    OPENING
}
