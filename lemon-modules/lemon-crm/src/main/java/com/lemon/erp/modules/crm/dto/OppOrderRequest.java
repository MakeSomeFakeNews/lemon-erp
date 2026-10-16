package com.lemon.erp.modules.crm.dto;

import java.time.LocalDate;

/**
 * 赢单商机生成销售订单（草稿）
 */
public record OppOrderRequest(Long warehouseId, LocalDate deliveryDate) {
}
