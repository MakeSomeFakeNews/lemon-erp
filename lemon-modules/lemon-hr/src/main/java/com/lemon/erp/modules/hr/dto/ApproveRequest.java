package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审批：approved 通过 / 拒绝，comment 审批意见
 */
public record ApproveRequest(@NotNull(message = "{hr.valid.selectApprovalResult}") Boolean approved,
                             @Size(max = 255, message = "{hr.valid.approvalCommentCannotExceedCharacters}") String comment) {
}
