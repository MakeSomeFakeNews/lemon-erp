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
        @NotNull(message = "请选择设备") Long equipmentId,
        @NotNull(message = "请选择类型") MaintenanceType type,
        @NotBlank(message = "请填写故障现象或保养内容") @Size(max = 255, message = "内容不能超过255个字符") String content,
        LocalDate planDate,
        @Size(max = 64, message = "处理人不能超过64个字符") String handler) {
}
