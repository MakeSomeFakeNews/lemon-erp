package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新建拣货波次 / 加入发货通知
 */
public record WaveRequest(
        Long warehouseId,
        @NotEmpty(message = "请选择发货通知") @Size(max = 100, message = "一个波次最多 100 张发货通知") List<@NotNull Long> deliveryIds,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
