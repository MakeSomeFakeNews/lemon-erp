package com.lemon.erp.modules.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 推进销售阶段；输单需要填写原因
 */
public record StageRequest(@NotBlank(message = "请选择销售阶段") String stage,
                           @Size(max = 255, message = "输单原因不能超过255个字符") String lostReason) {
}
