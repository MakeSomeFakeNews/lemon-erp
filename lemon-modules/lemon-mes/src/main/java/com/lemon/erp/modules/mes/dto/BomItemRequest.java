package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param qty      单位用量：生产 1 个产品需要的数量
 * @param lossRate 损耗率（%）
 * @param refDes   位号，如 R1,R2 或区间 C1-C8，见 {@link com.lemon.erp.modules.mes.model.RefDes}
 */
public record BomItemRequest(
        @NotNull(message = "请选择子件") Long materialId,
        @NotNull(message = "请填写用量")
        @DecimalMin(value = "0", inclusive = false, message = "用量必须大于0")
        @Digits(integer = 12, fraction = 6, message = "用量最多6位小数") BigDecimal qty,
        @DecimalMin(value = "0", message = "损耗率不能小于0") @DecimalMax(value = "100", message = "损耗率不能大于100") BigDecimal lossRate,
        @Size(max = 1000, message = "位号不能超过1000个字符") String refDes,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
