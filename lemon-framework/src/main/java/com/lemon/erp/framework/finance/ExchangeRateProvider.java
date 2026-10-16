package com.lemon.erp.framework.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率：供应链下外币订单时取记账汇率，由财务模块实现（{@code FinCurrencyService}）
 */
public interface ExchangeRateProvider {

    /** 本位币代码 */
    String BASE = "CNY";

    /**
     * 外币在某日所在月份的汇率（1 外币 = rate 本位币），该月没有维护时取之前最近一个月的；本位币为 1
     *
     * @return 没有维护过汇率时返回 null
     */
    BigDecimal rate(String currency, LocalDate date);
}
