package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.OrderChangeAction;
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
 * 订单变更：MODIFY 修改已有明细的数量 / 单价 / 交期（为空表示不变），REMOVE 删除未出入库的明细，ADD 新增明细。
 * 单价为优惠前的含税单价（没有整单优惠时即订单单价）
 *
 * @param wholeDiscount 变更后的整单优惠（含税），为空表示不变；按变更后未出入库部分的金额重新分摊。只改优惠时明细可以为空
 */
public record OrderChangeRequest(
        @NotBlank(message = "{scm.valid.enterChangeReason}") @Size(max = 255, message = "{scm.valid.changeReasonCannotExceedCharacters}") String reason,
        @NotNull(message = "{scm.valid.enterChangeDetails}") @Size(max = 200) List<@Valid @NotNull Line> lines,
        @DecimalMin(value = "0", message = "{scm.valid.wholeDiscountCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") BigDecimal wholeDiscount) {

    /** 不修改整单优惠 */
    public OrderChangeRequest(String reason, List<Line> lines) {
        this(reason, lines, null);
    }

    public record Line(
            @NotNull(message = "{scm.valid.invalidChangeAction}") OrderChangeAction action,
            Long orderItemId,
            Long materialId,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal taxRate,
            LocalDate deliveryDate) {
    }
}
