package com.lemon.erp.modules.mes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补料申请
 *
 * @param reason 字典 mes_supplement_reason
 */
public record SupplementRequest(
        @NotNull(message = "请选择工单") Long woId,
        @NotBlank(message = "请选择补料原因") @Size(max = 16, message = "补料原因不正确") String reason,
        @Size(max = 255, message = "说明不能超过255个字符") String remark,
        @NotEmpty(message = "请添加补料明细") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量")
            @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty) {
    }
}
