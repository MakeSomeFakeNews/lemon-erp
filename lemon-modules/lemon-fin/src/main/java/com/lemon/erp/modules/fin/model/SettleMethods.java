package com.lemon.erp.modules.fin.model;

/**
 * 结算方式（业务字典 fin_settle_method，租户可新增取值）中程序依赖的内置取值
 */
public final class SettleMethods {

    /** 银行转账 */
    public static final String TRANSFER = "TRANSFER";
    /** 票据：收票、背书转让、开具应付票据生成的收付款单，只能在票据管理中删除 */
    public static final String NOTE = "NOTE";

    private SettleMethods() {
    }
}
