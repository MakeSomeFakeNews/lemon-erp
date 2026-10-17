package com.lemon.erp.modules.workflow.dto;

import jakarta.validation.constraints.Size;

/**
 * 撤回、终止流程
 */
public record ReasonRequest(@Size(max = 500, message = "{workflow.valid.reasonCannotExceedCharacters}") String reason) {
}
