package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改销售报价单（草稿）
 */
public record QuotationRequest(
        @NotNull(message = "请选择客户") Long customerId,
        Long opportunityId,
        @NotNull(message = "请选择报价日期") LocalDate quoteDate,
        LocalDate validTo,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加明细") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            @NotNull(message = "请填写单价") @DecimalMin(value = "0", message = "单价不能小于0")
            @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
            @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate,
            @Size(max = 255, message = "备注不能超过255个字符") String remark) {
    }
}
