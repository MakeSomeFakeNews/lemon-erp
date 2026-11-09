package com.lemon.erp.modules.scm.event;

import com.lemon.erp.modules.scm.model.OrderKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单关闭时的尾差调整（与关闭同一事务）：部分出入库的订单行关闭后，已出入库金额按 行金额 × 已出入库数量 / 订单数量 调整，
 * 财务据此生成应收 / 应付调整单（来源 ORDER_ADJUST），保证应收应付与订单口径一致。没有差额时不发布
 *
 * @param date          调整日期（关闭日期）
 * @param amount        调整金额（本位币，含税），正数为蓝字增加应收 / 应付，负数为红字冲减
 * @param taxAmount     其中税额（本位币，与 amount 同号）
 * @param currency      订单币种
 * @param exchangeRate  订单汇率（本位币为 1）
 * @param amountFc      调整金额（订单币种）
 */
public record OrderCloseAdjustedEvent(OrderKind kind, Long orderId, String orderNo, Long partnerId, LocalDate date, BigDecimal amount,
                                      BigDecimal taxAmount, String currency, BigDecimal exchangeRate, BigDecimal amountFc) {
}
