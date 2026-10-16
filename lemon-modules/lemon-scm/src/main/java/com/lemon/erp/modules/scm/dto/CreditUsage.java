package com.lemon.erp.modules.scm.dto;

import java.math.BigDecimal;

/**
 * 客户信用占用
 *
 * @param creditLimit 信用额度，为空表示不限
 * @param receivable  应收余额（未核销）
 * @param openOrders  未完成订单的未发货金额
 */
public record CreditUsage(BigDecimal creditLimit, BigDecimal receivable, BigDecimal openOrders) {

    /** 可用额度，不限额度时为空 */
    public BigDecimal available() {
        return creditLimit == null ? null : creditLimit.subtract(receivable).subtract(openOrders);
    }
}
