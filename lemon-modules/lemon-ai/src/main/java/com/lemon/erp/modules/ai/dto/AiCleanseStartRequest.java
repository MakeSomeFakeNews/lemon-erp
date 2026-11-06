package com.lemon.erp.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发起数据整理
 *
 * @param sourceKey 数据类型（AiCleanseSource.key）
 * @param keyword   编码或名称关键字（整理范围），为空表示全部
 */
public record AiCleanseStartRequest(@NotBlank(message = "{ai.valid.cleanseSourceRequired}") String sourceKey,
                                    @Size(max = 64, message = "{ai.valid.cleanseKeywordTooLong}") String keyword) {
}
