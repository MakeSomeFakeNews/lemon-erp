package com.lemon.erp.modules.bi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存自定义报表：分析配置 + 名称
 */
public record ReportRequest(
        @NotBlank(message = "报表名称不能为空") @Size(max = 64, message = "报表名称不能超过64个字符") String name,
        @NotNull(message = "请设置分析配置") @Valid PivotRequest config,
        Integer shared,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {
}
