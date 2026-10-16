package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 确认拣货：每个通知明细的拣货数量合计须等于通知数量
 */
public record PickRequest(@NotEmpty(message = "请填写拣货明细") @Size(max = 1000, message = "拣货明细过多") List<@Valid @NotNull Line> picks) {

    public record Line(
            @NotNull(message = "缺少通知明细") Long deliveryItemId,
            Long locationId,
            @Size(max = 64, message = "批次号不能超过64个字符") String batchNo,
            @NotNull(message = "请填写拣货数量") @DecimalMin(value = "0", inclusive = false, message = "拣货数量必须大于0") BigDecimal qty,
            String serialNos) {
    }
}
