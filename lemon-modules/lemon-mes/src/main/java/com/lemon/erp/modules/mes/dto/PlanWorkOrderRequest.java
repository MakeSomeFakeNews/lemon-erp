package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 计划转工单
 *
 * @param qty     工单数量，为空时取计划未转工单的数量
 * @param bomId   为空时用默认 BOM
 * @param planEnd 为空时取计划的需求日期
 */
public record PlanWorkOrderRequest(
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        Long bomId,
        LocalDate planStart,
        LocalDate planEnd,
        Long warehouseId) {
}
