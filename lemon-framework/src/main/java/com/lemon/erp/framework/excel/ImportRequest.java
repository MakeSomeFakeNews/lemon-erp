package com.lemon.erp.framework.excel;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Excel 导入请求：前端解析文件后按行提交，字典标签、名称等已转换为值和 ID
 */
public record ImportRequest<T>(
        @NotEmpty(message = "没有要导入的数据")
        @Size(max = ImportExecutor.MAX_ROWS, message = "一次最多导入{max}行") List<ImportRow<T>> rows) {
}
