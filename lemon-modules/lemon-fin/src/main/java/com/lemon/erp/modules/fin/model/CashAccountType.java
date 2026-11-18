package com.lemon.erp.modules.fin.model;

/**
 * 资金账户类型，与字典 fin_cash_account_type 对应。
 * 现金账户的入账科目在指定的现金科目下，其他类型在银行科目或其他货币资金科目下；现金账户没有对账单，不做银行对账
 */
public enum CashAccountType {
    CASH,
    BANK,
    WECHAT,
    ALIPAY,
    OTHER
}
