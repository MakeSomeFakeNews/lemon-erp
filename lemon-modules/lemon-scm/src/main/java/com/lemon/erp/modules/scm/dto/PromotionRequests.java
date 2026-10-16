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
            @NotBlank(message = "请填写促销名称") @Size(max = 64, message = "促销名称不能超过64个字符") String name,
            @NotNull(message = "请选择促销类型") PromoType promoType,
            @NotNull(message = "请选择开始日期") LocalDate beginDate,
            @NotNull(message = "请选择结束日期") LocalDate endDate,
            @Size(max = 16) String customerLevel,
            Long materialId,
            @Size(max = 16) String materialCategory,
            @DecimalMin(value = "0", message = "数量不能为负数") @Digits(integer = 14, fraction = 4) BigDecimal minQty,
            @Digits(integer = 3, fraction = 2) BigDecimal discountRate,
            @DecimalMin(value = "0", message = "金额不能为负数") @Digits(integer = 16, fraction = 2) BigDecimal thresholdAmount,
            @DecimalMin(value = "0", message = "金额不能为负数") @Digits(integer = 16, fraction = 2) BigDecimal reduceAmount,
            @Digits(integer = 14, fraction = 4) BigDecimal buyQty,
            Long giftMaterialId,
            @Digits(integer = 14, fraction = 4) BigDecimal giftQty,
            @Size(max = 255, message = "说明不能超过255个字符") String remark) {
    }

    /** 下单前预览：客户、订单日期、明细（原价）、返利抵扣 */
    public record Preview(
            @NotNull(message = "请选择客户") Long customerId,
            @NotNull(message = "请选择订单日期") LocalDate orderDate,
            @NotEmpty(message = "请添加明细") @Size(max = 500) List<@Valid @NotNull Line> items,
            @DecimalMin(value = "0", message = "返利抵扣不能为负数") BigDecimal rebateAmount) {
    }

    public record Line(@NotNull Long materialId, @NotNull @DecimalMin("0") BigDecimal qty, @NotNull @DecimalMin("0") BigDecimal price) {
    }
}
