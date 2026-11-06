package com.lemon.erp.modules.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量采纳 / 忽略建议
 *
 * @param ids 建议 ID
 */
public record AiCleanseIdsRequest(@NotEmpty(message = "{ai.valid.cleanseIdsRequired}")
                                  @Size(max = 200, message = "{ai.valid.cleanseIdsTooMany}") List<Long> ids) {
}
