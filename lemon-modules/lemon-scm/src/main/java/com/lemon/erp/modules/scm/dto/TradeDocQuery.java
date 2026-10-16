package com.lemon.erp.modules.scm.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 入库 / 出库 / 退货单列表查询条件
 *
 * @param docNo   单据号，模糊匹配
 * @param orderNo 订单号，模糊匹配
 */
public record TradeDocQuery(String docNo, String orderNo, Long orderId, Long partnerId, Long warehouseId,
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
