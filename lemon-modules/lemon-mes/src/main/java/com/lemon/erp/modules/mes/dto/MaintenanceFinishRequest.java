package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 完成维修 / 保养
 */
public record MaintenanceFinishRequest(
        @NotBlank(message = "请填写处理结果") @Size(max = 255, message = "处理结果不能超过255个字符") String result,
        @DecimalMin(value = "0", message = "费用不能小于0") BigDecimal cost,
        @Size(max = 64, message = "处理人不能超过64个字符") String handler) {
}
