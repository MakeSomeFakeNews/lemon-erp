package com.lemon.erp.modules.fin.model;

/**
 * 单据的调整项（issue #93）：销售金额 = 货款 − 整单优惠 − 本公司承担运费 − 收款折让 / 抹零 − 坏账核销，毛利 = 销售金额 − 成本；
 * 进货只有整单优惠和付款折让 / 抹零。按此顺序显示
 */
public enum AdjustItem {
    /** 整单优惠（不含税） */
    DISCOUNT,
    /** 本公司承担的运费（只有销售） */
    FREIGHT,
    /** 收款折让 / 抹零（销售）、付款折让 / 抹零（进货），不含税 */
    SETTLE_DISCOUNT,
    /** 坏账核销（不含税，只有销售） */
    BAD_DEBT
}
