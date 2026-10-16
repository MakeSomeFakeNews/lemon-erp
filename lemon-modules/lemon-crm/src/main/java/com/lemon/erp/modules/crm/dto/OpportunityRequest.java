package com.lemon.erp.modules.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改商机。有产品明细时金额为明细合计；赢单后生成销售订单需要产品明细。
 * 销售阶段通过「推进阶段」修改，新增时默认初步接洽
 *
 * @param probability 赢单概率，不填按阶段默认
 */
public record OpportunityRequest(
        @NotBlank(message = "商机名称不能为空") @Size(max = 128, message = "商机名称不能超过128个字符") String name,
        @NotNull(message = "请选择客户") Long customerId,
        Long contactId,
        @DecimalMin(value = "0", message = "金额不能小于0") @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
        String stage,
        @Min(value = 0, message = "赢单概率为0-100") @Max(value = 100, message = "赢单概率为0-100") Integer probability,
        LocalDate expectedDate,
        String source,
        Long campaignId,
        Long ownerId,
        @Size(max = 500, message = "备注不能超过500个字符") String remark,
        @Size(max = 100, message = "产品明细不能超过100行") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "请选择产品") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4, message = "数量最多4位小数") BigDecimal qty,
            @NotNull(message = "请填写单价") @DecimalMin(value = "0", message = "单价不能小于0")
            @Digits(integer = 14, fraction = 4, message = "单价最多4位小数") BigDecimal price,
            @DecimalMin(value = "0", message = "税率不能小于0") @DecimalMax(value = "100", message = "税率不能大于100") BigDecimal taxRate) {
    }
}
