package com.lemon.erp.modules.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 转交任务
 *
 * @param userId 转交给谁
 */
public record TaskTransferRequest(@NotNull(message = "{workflow.valid.selectTransferee}") Long userId,
                                  @Size(max = 500, message = "{workflow.valid.transferNoteCannotExceedCharacters}") String comment) {
}
