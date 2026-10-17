package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 登记设备停机
 */
public record StopRequest(
        @NotNull(message = "{mes.valid.selectEquipment}") Long equipmentId,
        @NotNull(message = "{mes.valid.selectStartTime}") LocalDateTime startTime,
        @NotNull(message = "{mes.valid.selectEndTime}") LocalDateTime endTime,
        @NotBlank(message = "{mes.valid.selectStopReason}") String reason,
        @Size(max = 255, message = "{mes.valid.descriptionCannotExceedCharacters}") String remark) {
}
