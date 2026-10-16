package com.lemon.erp.framework.excel;

import java.util.List;

/**
 * 导入结果：有错误时整批不导入
 *
 * @param total    行数
 * @param imported 导入成功的行数（有错误时为 0）
 * @param errors   出错的行
 */
public record ImportResult(int total, int imported, List<RowError> errors) {

    public record RowError(Integer rowNum, String message) {
    }
}
