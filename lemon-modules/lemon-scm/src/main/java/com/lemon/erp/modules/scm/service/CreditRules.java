package com.lemon.erp.modules.scm.service;

import com.lemon.erp.modules.scm.model.CreditControl;

import java.math.BigDecimal;

/**
 * 客户信用额度的判断规则（纯计算，不访问数据库）
 */
public final class CreditRules {

    private CreditRules() {
    }

    /** 审核销售订单时是否校验信用额度：只有「审核和出库都控制」时校验 */
    public static boolean checksOnApprove(CreditControl control) {
        return control == CreditControl.ALL;
    }

    /** 出库前是否校验信用额度：「不控制」之外都校验 */
    public static boolean checksOnShipment(CreditControl control) {
        return control != CreditControl.NONE;
    }

    /**
     * 审核时的信用占用：其他未完成订单的未发货金额 + 应收余额 + 本订单金额（按汇率折为本位币，汇率为空按 1）
     */
    public static BigDecimal usageOnApprove(BigDecimal open, BigDecimal receivable, BigDecimal orderAmount, BigDecimal exchangeRate) {
        return open.add(receivable).add(orderAmount.multiply(exchangeRate == null ? BigDecimal.ONE : exchangeRate));
    }

    /** 出库前的信用占用：全部未完成订单的未发货金额 + 应收余额 */
    public static BigDecimal usageOnShipment(BigDecimal open, BigDecimal receivable) {
        return open.add(receivable);
    }

    /** 占用是否超过额度（等于额度不算超过） */
    public static boolean exceeds(BigDecimal used, BigDecimal creditLimit) {
        return used.compareTo(creditLimit) > 0;
    }
}
