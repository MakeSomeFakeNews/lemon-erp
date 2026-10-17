package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.PromoType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 促销活动的请求
 */
public final class PromotionRequests {

    private PromotionRequests() {
    }

    /**
     * 新增 / 修改促销；按类型填写：折扣 discountRate（可选 minQty），满减 thresholdAmount、reduceAmount，买赠 buyQty、giftMaterialId、giftQty
     *
     * @param materialId       适用物料，为空按分类
     * @param materialCategory 适用分类，物料和分类都为空适用全部物料
     */
    public record Save(
            @NotBlank(message = "{scm.valid.enterPromotionName}") @Size(max = 64, message = "{scm.valid.promotionNameCannotExceedCharacters}") String name,
            @NotNull(message = "{scm.valid.selectPromotionType}") PromoType promoType,
            @NotNull(message = "{scm.valid.selectStartDate}") LocalDate beginDate,
            @NotNull(message = "{scm.valid.selectEndDate}") LocalDate endDate,
            @Size(max = 16) String customerLevel,
            Long materialId,
            @Size(max = 16) String materialCategory,
            @DecimalMin(value = "0", message = "{scm.valid.quantityCannotNegative}") @Digits(integer = 14, fraction = 4) BigDecimal minQty,
            @Digits(integer = 3, fraction = 2) BigDecimal discountRate,
            @DecimalMin(value = "0", message = "{scm.valid.amountCannotNegative}") @Digits(integer = 16, fraction = 2) BigDecimal thresholdAmount,
            @DecimalMin(value = "0", message = "{scm.valid.amountCannotNegative}") @Digits(integer = 16, fraction = 2) BigDecimal reduceAmount,
            @Digits(integer = 14, fraction = 4) BigDecimal buyQty,
            Long giftMaterialId,
            @Digits(integer = 14, fraction = 4) BigDecimal giftQty,
            @Size(max = 255, message = "{scm.valid.descriptionCannotExceedCharacters}") String remark) {
    }

    /** 下单前预览：客户、订单日期、明细（原价）、返利抵扣 */
    public record Preview(
            @NotNull(message = "{scm.valid.selectCustomer}") Long customerId,
            @NotNull(message = "{scm.valid.selectOrderDate}") LocalDate orderDate,
            @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500) List<@Valid @NotNull Line> items,
            @DecimalMin(value = "0", message = "{scm.valid.rebateDeductionCannotNegative}") BigDecimal rebateAmount) {
    }

    public record Line(@NotNull Long materialId, @NotNull @DecimalMin("0") BigDecimal qty, @NotNull @DecimalMin("0") BigDecimal price) {
    }
}
