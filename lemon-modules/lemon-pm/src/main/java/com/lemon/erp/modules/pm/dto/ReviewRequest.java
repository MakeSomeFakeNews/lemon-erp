package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 工时审批：通过 / 驳回（驳回需要原因）
 */
public record ReviewRequest(@NotEmpty(message = "请选择工时") List<Long> ids,
                            @NotNull(message = "请选择审批结果") Boolean approved,
                            @Size(max = 255, message = "驳回原因不能超过255个字符") String reason) {
}
