package com.lemon.erp.modules.fin.model;

/**
 * 结算方式，与字典 fin_settle_method 对应
 */
public enum SettleMethod {
    TRANSFER,
    CASH,
    /** 票据：收票、背书转让、开具应付票据生成的收付款单，只能在票据管理中删除 */
    NOTE
}
