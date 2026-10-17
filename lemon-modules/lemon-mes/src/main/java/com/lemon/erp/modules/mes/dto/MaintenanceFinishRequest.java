package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 完成维修 / 保养
 */
public record MaintenanceFinishRequest(
        @NotBlank(message = "{mes.valid.enterResult}") @Size(max = 255, message = "{mes.valid.resultCannotExceedCharacters}") String result,
        @DecimalMin(value = "0", message = "{mes.valid.costCannotLess}") BigDecimal cost,
        @Size(max = 64, message = "{mes.valid.handlerCannotExceedCharacters}") String handler) {
}
