package com.lemon.erp.modules.pm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 工时审批：通过 / 驳回（驳回需要原因）
 */
public record ReviewRequest(@NotEmpty(message = "{pm.valid.selectTimesheets}") List<Long> ids,
                            @NotNull(message = "{pm.valid.selectApprovalResult}") Boolean approved,
                            @Size(max = 255, message = "{pm.valid.rejectionReasonCannotExceedCharacters}") String reason) {
}
