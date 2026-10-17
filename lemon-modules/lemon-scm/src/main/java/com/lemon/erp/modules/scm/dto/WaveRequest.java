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
        @NotEmpty(message = "{scm.valid.selectDeliveryNotice}") @Size(max = 100, message = "{scm.valid.waveContainMostDeliveryNotices}") List<@NotNull Long> deliveryIds,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
}
