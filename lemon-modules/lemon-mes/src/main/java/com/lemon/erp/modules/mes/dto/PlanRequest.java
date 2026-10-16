package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanRequest(
        @NotNull(message = "请选择产品") Long materialId,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        @NotNull(message = "请选择需求日期") LocalDate dueDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
