package com.lemon.erp.modules.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 导入字段对应：模板字段、Excel 表头和前几行样例
 *
 * @param columns 模板字段
 * @param headers Excel 第一行的表头（按列顺序）
 * @param samples 前几行数据（每行按列顺序，最多 5 行）
 */
public record AiImportMappingRequest(
        @NotEmpty(message = "{ai.valid.importColumnsRequired}") @Size(max = 100, message = "{ai.valid.importTooManyColumns}") List<@Valid Column> columns,
        @NotEmpty(message = "{ai.valid.importHeadersRequired}") @Size(max = 100, message = "{ai.valid.importTooManyColumns}") List<@Size(max = 100, message = "{ai.valid.importColumnTooLong}") String> headers,
        @Size(max = 5, message = "{ai.valid.importTooManySamples}") List<List<String>> samples) {

    /**
     * 模板字段
     *
     * @param key      字段
     * @param title    名称（模板表头）
     * @param required 是否必填
     */
    public record Column(@NotBlank(message = "{ai.valid.importColumnsRequired}") @Size(max = 64, message = "{ai.valid.importColumnTooLong}") String key,
                         @NotBlank(message = "{ai.valid.importColumnsRequired}") @Size(max = 64, message = "{ai.valid.importColumnTooLong}") String title,
                         boolean required) {
    }
}
