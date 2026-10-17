package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务报表行
 *
 * @param lineNo   行次，分组标题行为空
 * @param level    缩进层级：0 标题 / 合计，1 项目，2 其中项
 * @param title    是否分组标题行（如「流动资产：」），没有金额
 * @param formula  取数说明（科目或行次公式）
 * @param values   与报表列一一对应
 * @param accounts 构成科目（合计行为空）
 */
public record FinancialRow(Integer lineNo, String name, int level, boolean bold, boolean title, String formula,
                           List<BigDecimal> values, List<FinancialRowAccount> accounts) {
}
