package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param ratio    替代 1 个主料需要的替代料数量
 * @param priority 越小越优先，默认 1
 */
public record SubstituteRequest(
        @NotNull(message = "请选择替代料") Long substituteId,
        @NotNull(message = "请填写替代比例")
        @DecimalMin(value = "0", inclusive = false, message = "替代比例必须大于0")
        @Digits(integer = 12, fraction = 6, message = "替代比例最多6位小数") BigDecimal ratio,
        Integer priority,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
