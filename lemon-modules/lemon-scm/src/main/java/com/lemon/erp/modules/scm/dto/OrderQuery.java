package com.lemon.erp.modules.scm.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 订单列表查询条件
 *
 * @param orderNo   订单号，模糊匹配
 * @param keyword   物料编码或名称：包含该物料的订单
 * @param beginDate 订单日期范围
 */
public record OrderQuery(String orderNo, Long partnerId, String status, String keyword,
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
