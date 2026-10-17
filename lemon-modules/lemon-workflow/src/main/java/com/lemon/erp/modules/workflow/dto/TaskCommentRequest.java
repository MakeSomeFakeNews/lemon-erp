package com.lemon.erp.modules.workflow.dto;

import jakarta.validation.constraints.Size;

/**
 * 同意、拒绝任务（拒绝时审批意见必填）
 */
public record TaskCommentRequest(@Size(max = 500, message = "{workflow.valid.commentCannotExceedCharacters}") String comment) {
}
