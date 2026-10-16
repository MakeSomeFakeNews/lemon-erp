package com.lemon.erp.modules.scm.dto;

/**
 * 发运单列表查询条件
 *
 * @param keyword 发运单号、运单号或出库单号，模糊匹配
 */
public record ShipmentQuery(String keyword, Long customerId, String carrier, String status) {
}
