package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改生产工单（草稿）
 *
 * @param bomId 不填使用产品的默认 BOM
 */
public record WorkOrderRequest(
        @NotNull(message = "请选择产品") Long materialId,
        Long bomId,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        LocalDate planStart,
        LocalDate planEnd,
        Long warehouseId,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
