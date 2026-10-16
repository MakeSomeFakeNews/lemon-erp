package com.lemon.erp.modules.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 转交任务
 *
 * @param userId 转交给谁
 */
public record TaskTransferRequest(@NotNull(message = "请选择转交人") Long userId,
                                  @Size(max = 500, message = "转交说明不能超过500个字符") String comment) {
}
