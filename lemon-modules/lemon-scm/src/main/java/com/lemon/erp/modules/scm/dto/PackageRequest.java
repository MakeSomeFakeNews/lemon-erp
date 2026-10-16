package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 装一箱：重量、体积和箱内物料（发货通知明细 + 数量）
 */
public record PackageRequest(
        @DecimalMin(value = "0", message = "重量不能为负数") @Digits(integer = 7, fraction = 3, message = "重量格式不正确") BigDecimal weight,
        @DecimalMin(value = "0", message = "体积不能为负数") @Digits(integer = 6, fraction = 4, message = "体积格式不正确") BigDecimal volume,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请填写箱内物料") @Size(max = 200) List<@Valid @NotNull Line> items) {

    public record Line(
            @NotNull(message = "缺少发货通知明细") Long deliveryItemId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4) BigDecimal qty) {
    }
}
