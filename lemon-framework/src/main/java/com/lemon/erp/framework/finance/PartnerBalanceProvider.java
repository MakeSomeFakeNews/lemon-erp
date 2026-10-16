package com.lemon.erp.framework.finance;

import java.math.BigDecimal;

/**
 * 往来余额：供应链校验客户信用额度时需要应收余额，由财务模块实现（{@code FinPartnerBalanceProvider}）
 */
public interface PartnerBalanceProvider {

    /** 客户的应收余额：未核销的应收单金额合计（含期初） */
    BigDecimal receivableBalance(Long customerId);
}
