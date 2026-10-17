package com.lemon.erp.framework.excel;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Excel 导入请求：前端解析文件后按行提交，字典标签、名称等已转换为值和 ID
 */
public record ImportRequest<T>(
        @NotEmpty(message = "{framework.valid.noDataImport}")
        @Size(max = ImportExecutor.MAX_ROWS, message = "{framework.valid.upMaxRowsImportedTime}") List<ImportRow<T>> rows) {
}
