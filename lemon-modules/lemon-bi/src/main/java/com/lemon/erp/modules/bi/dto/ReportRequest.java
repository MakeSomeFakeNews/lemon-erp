package com.lemon.erp.modules.bi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存自定义报表：分析配置 + 名称
 */
public record ReportRequest(
        @NotBlank(message = "{bi.valid.reportNameRequired}") @Size(max = 64, message = "{bi.valid.reportNameCannotExceedCharacters}") String name,
        @NotNull(message = "{bi.valid.configureAnalysis}") @Valid PivotRequest config,
        Integer shared,
        @Size(max = 255, message = "{bi.valid.descriptionCannotExceedCharacters}") String remark) {
}
