package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 新增维修 / 保养单
 *
 */
public record MaintenanceRequest(
        @NotNull(message = "{mes.valid.selectEquipment}") Long equipmentId,
        @NotNull(message = "{mes.valid.selectType}") MaintenanceType type,
        @NotBlank(message = "{mes.valid.enterSymptomMaintenanceContent}") @Size(max = 255, message = "{mes.valid.contentCannotExceedCharacters}") String content,
        LocalDate planDate,
        @Size(max = 64, message = "{mes.valid.handlerCannotExceedCharacters}") String handler) {
}
