package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.RebateCalcMode;
import com.lemon.erp.modules.scm.model.RebateSettle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改返利协议（执行中）。种类（销售 / 采购）由接口决定：销售返利填客户，采购返利填供应商
 *
 * @param calcMode 计算方式，不填为阶梯比例
 * @param tiers    阶梯（阶梯比例时必填）：金额达到 minAmount 时返利率为 rate%，取达到的最高一档
 * @param items    返利项目（按项目填写金额时必填）
 */
public record RebateRequest(
        Long customerId,
        Long supplierId,
        @NotNull(message = "{scm.valid.selectStartDate}") LocalDate beginDate,
        @NotNull(message = "{scm.valid.selectEndDate}") LocalDate endDate,
        @NotNull(message = "{scm.valid.selectPayoutMethod}") RebateSettle settleMode,
        RebateCalcMode calcMode,
        @Size(max = 20, message = "{scm.valid.rebateTiersCannotExceed}") List<@Valid @NotNull Tier> tiers,
        @Size(max = 100, message = "{scm.valid.rebateItemsCannotExceed}") List<@Valid @NotNull Item> items,
        @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark) {

    public record Tier(
            @NotNull(message = "{scm.valid.enterSalesAmount}") @DecimalMin(value = "0", message = "{scm.valid.salesAmountCannotNegative}") @Digits(integer = 16, fraction = 2) BigDecimal minAmount,
            @NotNull(message = "{scm.valid.enterRebateRate}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.rebateRateMustGreater}")
            @DecimalMax(value = "100", message = "{scm.valid.rebateRateCannotExceed}") BigDecimal rate) {
    }

    /**
     * @param brand    品牌（业务字典 wms_material_brand），可为空
     * @param category 物料分类（业务字典 wms_material_category），可为空
     */
    public record Item(
            @NotBlank(message = "{scm.valid.enterRebateItemName}") @Size(max = 64, message = "{scm.valid.rebateItemNameTooLong}") String itemName,
            @Size(max = 64) String brand,
            @Size(max = 32) String category,
            @NotNull(message = "{scm.valid.enterRebateItemAmount}") @DecimalMin(value = "0.01", message = "{scm.valid.rebateItemAmountMustGreater}")
            @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark) {
    }
}
