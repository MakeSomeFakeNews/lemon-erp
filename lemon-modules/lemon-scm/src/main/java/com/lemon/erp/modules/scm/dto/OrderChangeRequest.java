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
        @NotBlank(message = "请填写变更原因") @Size(max = 255, message = "变更原因不能超过255个字符") String reason,
        @NotEmpty(message = "请填写变更内容") @Size(max = 200) List<@Valid @NotNull Line> lines) {

    public record Line(
            @NotBlank @Pattern(regexp = "^(ADD|MODIFY|REMOVE)$", message = "变更操作不正确") String action,
            Long orderItemId,
            Long materialId,
            @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0") @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @DecimalMin(value = "0", message = "单价不能小于0") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal taxRate,
            LocalDate deliveryDate) {
    }
}
