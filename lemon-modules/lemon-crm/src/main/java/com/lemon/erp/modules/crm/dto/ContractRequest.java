package com.lemon.erp.modules.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售合同（草稿）与回款计划
 */
public record ContractRequest(
        @NotBlank(message = "请填写合同名称") @Size(max = 128, message = "合同名称不能超过128个字符") String name,
        @NotNull(message = "请选择客户") Long customerId,
        Long opportunityId,
        Long quotationId,
        @NotNull(message = "请选择签订日期") LocalDate signDate,
        LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "请填写合同金额") @DecimalMin(value = "0.01", message = "合同金额必须大于0")
        @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        Long ownerId,
        @Size(max = 500, message = "备注不能超过500个字符") String remark,
        @Size(max = 24, message = "回款计划不能超过24期") List<@Valid @NotNull Plan> plans) {

    public record Plan(@NotNull(message = "请选择计划回款日期") LocalDate planDate,
                       @NotNull(message = "请填写计划回款金额") @DecimalMin(value = "0.01", message = "回款金额必须大于0")
                       @Digits(integer = 16, fraction = 2) BigDecimal amount,
                       @Size(max = 255) String remark) {
    }
}
