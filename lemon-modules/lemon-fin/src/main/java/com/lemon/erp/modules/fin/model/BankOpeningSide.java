package com.lemon.erp.modules.fin.model;

/**
 * 银行账户的期初未达账项，与字典 fin_bank_opening_side 对应
 */
public enum BankOpeningSide {
    /** 企业已记账、银行未记账（企业已收银行未收、企业已付银行未付）：启用后与银行对账单流水勾对 */
    BOOK,
    /** 银行已记账、企业未记账（银行已收企业未收、银行已付企业未付）：启用后与企业的凭证分录勾对 */
    BANK
}
