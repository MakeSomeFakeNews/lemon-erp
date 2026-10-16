package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审批：approved 通过 / 拒绝，comment 审批意见
 */
public record ApproveRequest(@NotNull(message = "请选择审批结果") Boolean approved,
                             @Size(max = 255, message = "审批意见不能超过255个字符") String comment) {
}
