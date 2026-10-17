package com.lemon.erp.modules.scm.event;

import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.scm.model.TradeKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购入库 / 退货、销售出库 / 退货已确认（库存已更新），在同一事务中同步发布。
 * 财务模块监听后生成应付 / 应收单；监听器抛异常会回滚整张单据。
 *
 * @param kind        出入库种类
 * @param totalAmount 价税合计，退货为负数
 * @param taxAmount   税额，退货为负数
 */
public record TradeDocConfirmedEvent(Long docId, TradeKind kind, String docNo, Long partnerId, LocalDate bizDate,
                                     BigDecimal totalAmount, BigDecimal taxAmount, String currency, BigDecimal exchangeRate,
                                     BigDecimal totalAmountFc) {

    /** 本位币单据 */
    public TradeDocConfirmedEvent(Long docId, TradeKind kind, String docNo, Long partnerId, LocalDate bizDate, BigDecimal totalAmount,
                                  BigDecimal taxAmount) {
        this(docId, kind, docNo, partnerId, bizDate, totalAmount, taxAmount, ExchangeRateProvider.BASE, BigDecimal.ONE, totalAmount);
    }
}
