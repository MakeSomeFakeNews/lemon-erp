package com.lemon.erp.framework.excel;

/**
 * 导入的一行
 *
 * @param rowNum Excel 行号，用于提示错误位置
 * @param data   行数据
 */
public record ImportRow<T>(Integer rowNum, T data) {
}
