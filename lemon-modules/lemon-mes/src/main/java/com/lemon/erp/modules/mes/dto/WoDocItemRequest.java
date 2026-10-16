package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param materialId 领料 / 退料为工单用料中的物料，完工入库为工单产品
 */
public record WoDocItemRequest(
        @NotNull(message = "请选择物料") Long materialId,
        Long locationId,
        @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
        LocalDate productionDate,
        LocalDate expiryDate,
        @NotNull(message = "请填写数量")
        @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
        String serialNos) {
}
