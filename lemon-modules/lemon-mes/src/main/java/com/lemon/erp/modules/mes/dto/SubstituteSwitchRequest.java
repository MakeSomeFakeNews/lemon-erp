package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 工单用料切换替代料
 *
 * @param qty 被替代的主料数量（不超过主料待领数量），替代料需求 = qty × 替代比例
 */
public record SubstituteSwitchRequest(
        @NotNull(message = "请选择工单") Long woId,
        @NotNull(message = "请选择主料") Long materialId,
        @NotNull(message = "请选择替代料") Long substituteId,
        @NotNull(message = "请填写替代数量")
        @DecimalMin(value = "0", inclusive = false, message = "替代数量必须大于0")
        @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty) {
}
