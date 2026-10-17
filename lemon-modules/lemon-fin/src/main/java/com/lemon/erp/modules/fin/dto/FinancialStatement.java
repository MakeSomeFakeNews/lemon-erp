package com.lemon.erp.modules.fin.dto;

import java.util.List;

/**
 * 财务报表（资产负债表、利润表）
 *
 * @param formNo   表号，如 会小企01表
 * @param columns  金额列名称
 * @param rows     报表行；资产负债表为左侧（资产）
 * @param right    资产负债表右侧（负债和所有者权益），利润表为空
 * @param balanced 资产负债表是否平衡，利润表为空
 */
public record FinancialStatement(String title, String formNo, List<String> columns, List<FinancialRow> rows, List<FinancialRow> right,
                        List<FinancialMetric> metrics, Boolean balanced) {
}
