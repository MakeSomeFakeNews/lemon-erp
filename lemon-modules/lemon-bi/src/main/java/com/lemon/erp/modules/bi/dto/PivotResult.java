package com.lemon.erp.modules.bi.dto;

import java.util.List;
import java.util.Map;

/**
 * 透视结果
 *
 * @param cells     每个（行、列）组合一条：r、c、各指标
 * @param rowTotals 每行合计（不分列）
 * @param colTotals 每列合计（不分行）
 * @param total     总计
 * @param labels    维度值名称：rows / cols → 值 → 名称（ID 类维度）
 * @param begin     实际查询的日期范围
 */
public record PivotResult(List<Map<String, Object>> cells, List<Map<String, Object>> rowTotals,
                          List<Map<String, Object>> colTotals, Map<String, Object> total,
                          Map<String, Map<String, String>> labels, String begin, String end) {
}
