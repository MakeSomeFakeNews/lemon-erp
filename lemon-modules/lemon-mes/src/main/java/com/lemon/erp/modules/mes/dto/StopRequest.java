package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.StopReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 登记设备停机
 */
public record StopRequest(
        @NotNull(message = "请选择设备") Long equipmentId,
        @NotNull(message = "请选择开始时间") LocalDateTime startTime,
        @NotNull(message = "请选择结束时间") LocalDateTime endTime,
        @NotNull(message = "请选择停机原因") StopReason reason,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {
}
