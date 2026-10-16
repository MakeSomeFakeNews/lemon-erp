package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 录入一个供应商的报价：覆盖该供应商之前的报价；不报价的明细不传
 */
public record InquiryQuoteRequest(
        @NotNull(message = "请选择供应商") Long supplierId,
        @NotNull(message = "请填写报价") @Size(max = 200, message = "报价不能超过200行") List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotNull(message = "缺少询价明细") Long itemId,
            @NotNull(message = "请填写单价") @DecimalMin(value = "0", message = "单价不能小于0")
            @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
            @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
            @Min(value = 0, message = "交期不能小于0") @Max(value = 365, message = "交期不能超过365天") Integer leadDays,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
