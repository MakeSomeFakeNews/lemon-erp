package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单变更：MODIFY 修改已有明细的数量 / 单价 / 交期（为空表示不变），REMOVE 删除未出入库的明细，ADD 新增明细
 */
public record OrderChangeRequest(
        @NotBlank(message = "{scm.valid.enterChangeReason}") @Size(max = 255, message = "{scm.valid.changeReasonCannotExceedCharacters}") String reason,
        @NotEmpty(message = "{scm.valid.enterChangeDetails}") @Size(max = 200) List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotBlank @Pattern(regexp = "^(ADD|MODIFY|REMOVE)$", message = "{scm.valid.invalidChangeAction}") String action,
            Long orderItemId,
            Long materialId,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal taxRate,
            LocalDate deliveryDate) {
    }
}
