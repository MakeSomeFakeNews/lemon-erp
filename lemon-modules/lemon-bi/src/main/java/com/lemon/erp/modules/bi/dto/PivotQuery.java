package com.lemon.erp.modules.bi.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 透视查询参数（传给 BiOlapMapper），维度编码已按数据集白名单校验
 *
 * @param row     行维度，为空汇总为一行
 * @param col     列维度，为空不分列
 * @param latest  库存分析没有指定日期时只取最新快照
 * @param filters 维度编码 → 取值列表
 */
public record PivotQuery(String row, String col, LocalDate begin, LocalDate end, Integer beginPeriod, Integer endPeriod,
                         boolean latest, Map<String, List<String>> filters) {
}
