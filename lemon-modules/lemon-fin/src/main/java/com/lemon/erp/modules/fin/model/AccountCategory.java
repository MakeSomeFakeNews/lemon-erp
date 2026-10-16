package com.lemon.erp.modules.fin.model;

/**
 * 科目类别，与字典 fin_account_category 对应
 */
public enum AccountCategory {
    ASSET,
    LIABILITY,
    EQUITY,
    COST,
    /** 损益类：收入（贷方）、费用（借方），期末结转到本年利润 */
    PROFIT
}
