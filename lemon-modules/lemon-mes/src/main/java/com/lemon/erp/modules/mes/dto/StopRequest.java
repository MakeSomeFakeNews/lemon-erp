package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.StopReason;
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
        @NotNull(message = "{mes.valid.selectStopReason}") StopReason reason,
        @Size(max = 255, message = "{mes.valid.descriptionCannotExceedCharacters}") String remark) {
}
