package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.pm.model.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目核算：收入按完工百分比确认，成本 = 人工（已审批工时）+ 费用；挣值分析 PV / EV / AC
 *
 * @param contractNet  合同金额（不含税）
 * @param revenue      已确认收入 = 不含税合同额 × 完工进度
 * @param plannedRate  计划进度（%）= 已过天数 / 计划工期
 * @param pv           计划价值 = 成本预算 × 计划进度
 * @param ev           挣值 = 成本预算 × 完工进度
 * @param spi          进度绩效指数 = EV / PV，小于 1 表示进度落后
 * @param cpi          成本绩效指数 = EV / AC，小于 1 表示成本超支
 * @param eac          完工估算 = 成本预算 / CPI
 */
public record ProjectAccounting(Long projectId, String code, String name, ProjectStatus status, String customerName, String managerName,
                                LocalDate planStart, LocalDate planEnd, BigDecimal progress, BigDecimal plannedRate,
                                BigDecimal contractAmount, BigDecimal contractNet, BigDecimal budget, BigDecimal laborCost,
                                BigDecimal expenseCost, BigDecimal actualCost, BigDecimal revenue, BigDecimal grossProfit,
                                BigDecimal grossMargin, BigDecimal billed, BigDecimal received,
                                BigDecimal pv, BigDecimal ev, BigDecimal spi, BigDecimal cpi, BigDecimal eac) {
}
