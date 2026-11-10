package com.lemon.erp.modules.fin.model;

/**
 * 账户调拨手续费的承担方，与字典 fin_fee_bearer 对应：转出账户（多付出手续费）、转入账户（少收到手续费）
 */
public enum FeeBearer {
    FROM,
    TO
}
