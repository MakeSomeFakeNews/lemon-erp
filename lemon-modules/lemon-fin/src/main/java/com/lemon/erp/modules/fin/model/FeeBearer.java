package com.lemon.erp.modules.fin.model;

/**
 * 账户调拨手续费的支付账户，与字典 fin_fee_bearer 对应：转出账户支付（多付出手续费）、转入账户支付（少收到手续费）
 */
public enum FeeBearer {
    FROM,
    TO
}
