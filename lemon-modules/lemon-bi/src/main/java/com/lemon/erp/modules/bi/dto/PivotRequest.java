package com.lemon.erp.modules.bi.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 多维分析请求，也是自定义报表保存的配置
 *
 * @param cube     数据集（BiCube）
 * @param row      行维度
 * @param col      列维度，选填
 * @param measures 显示的指标，为空显示全部
 * @param range    相对日期范围：THIS_MONTH、THIS_QUARTER、THIS_YEAR、LAST_12M、ALL；为空时使用 begin / end
 * @param filters  维度筛选：维度编码 → 取值列表
 * @param chart    图表类型：table、bar、line、pie（前端使用）
 */
public record PivotRequest(@NotBlank(message = "{bi.valid.selectDataset}") String cube, String row, String col, List<String> measures,
                           String range, LocalDate begin, LocalDate end, Map<String, List<String>> filters, String chart) {
}
