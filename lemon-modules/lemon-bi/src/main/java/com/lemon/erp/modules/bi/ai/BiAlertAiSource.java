package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiAlertSource;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.model.BiAlertMetric;
import com.lemon.erp.modules.bi.model.BiCube;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import com.lemon.erp.modules.fin.dto.BudgetExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.bi.ai.BiReportPivots.dec;
import static com.lemon.erp.modules.bi.ai.BiReportPivots.ratio;

/**
 * 经营预警的「AI 分析」材料：规则的当前值（与「决策支持 → 预警」相同的计算）、近 6 个月趋势（毛利率）、
 * 与指标直接相关的明细（呆滞物料、超支的预算科目），以及相关的智能报告来源
 */
@Component
@RequiredArgsConstructor
public class BiAlertAiSource implements AiAlertSource {

    private static final int DETAIL_ROWS = 10;
    private static final int TREND_MONTHS = 6;

    /** 指标相关的报告来源（AI 模块按当前用户的权限取近 30 天快照） */
    private static final Map<BiAlertMetric, List<String>> RELATED = Map.of(
            BiAlertMetric.AR_OVERDUE, List.of("fin.finance", "bi.sales"),
            BiAlertMetric.STOCK_BELOW_SAFETY, List.of("bi.inventory", "bi.purchase", "bi.sales"),
            BiAlertMetric.STOCK_ABOVE_MAX, List.of("bi.inventory", "bi.purchase", "bi.sales"),
            BiAlertMetric.SLOW_MOVING, List.of("bi.inventory", "bi.sales", "bi.purchase"),
            BiAlertMetric.GROSS_MARGIN, List.of("bi.sales", "bi.purchase"),
            BiAlertMetric.WO_LATE, List.of("mes.production", "mes.quality"),
            BiAlertMetric.BUDGET_OVERRUN, List.of("fin.finance"),
            BiAlertMetric.TICKET_OVERDUE, List.of(),
            BiAlertMetric.TASK_OVERDUE, List.of());

    private final BiDecisionService decisionService;
    private final BiReportPivots pivots;

    @Override
    public String[] permissions() {
        // 与「预警」接口一致
        return new String[]{"bi:decision:list", "bi:cockpit:list"};
    }

    @Override
    public AiAlert alert(Long ruleId) {
        BiInsight.AlertResult a = decisionService.alerts().stream().filter(r -> r.ruleId().equals(ruleId)).findFirst().orElse(null);
        if (a == null) {
            return null;
        }
        BiAlertMetric metric = BiAlertMetric.of(a.metric());
        return new AiAlert(a.ruleId(), a.name(), a.metric(), a.metricLabel(), a.unit(), a.operator().name(), a.threshold(),
                a.level().name(), a.value(), a.triggered(), a.remark(), trend(metric), details(metric),
                RELATED.getOrDefault(metric, List.of()));
    }

    /** 毛利率：近 6 个月（含本月）的销售额、毛利、毛利率 */
    private List<?> trend(BiAlertMetric metric) {
        if (metric != BiAlertMetric.GROSS_MARGIN) {
            return List.of();
        }
        YearMonth thisMonth = YearMonth.now();
        PivotResult r = pivots.pivot(BiCube.SALES, "period", thisMonth.minusMonths(TREND_MONTHS - 1L).atDay(1), LocalDate.now());
        return r.rowTotals().stream()
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("r"))))
                .map(m -> new MonthMargin(String.valueOf(m.get("r")), dec(m, "amount"), dec(m, "profit"), ratio(m, "margin")))
                .toList();
    }

    private List<?> details(BiAlertMetric metric) {
        if (metric == BiAlertMetric.SLOW_MOVING) {
            return decisionService.slowMoving(BiDecisionService.SLOW_DAYS).stream().limit(DETAIL_ROWS)
                    .map(r -> new BiInventoryReportSource.SlowItem(r.getCode(), r.getName(), r.getQty(), r.getAmount(), r.getIdleDays()))
                    .toList();
        }
        if (metric == BiAlertMetric.BUDGET_OVERRUN) {
            int month = LocalDate.now().getMonthValue();
            return decisionService.overBudget().stream().limit(DETAIL_ROWS)
                    .map(e -> overrun(e, month)).toList();
        }
        return List.of();
    }

    private static BudgetOverrun overrun(BudgetExecution e, int month) {
        BigDecimal budget = BiDecisionService.ytd(e.budgets(), month);
        BigDecimal actual = BiDecisionService.ytd(e.actuals(), month);
        return new BudgetOverrun(e.deptName(), e.accountCode(), e.accountName(), budget, actual, actual.subtract(budget));
    }

    /**
     * @param period    月份 yyyyMM
     * @param marginPct 毛利率（%）
     */
    public record MonthMargin(String period, BigDecimal salesAmount, BigDecimal grossProfit, BigDecimal marginPct) {
    }

    /** 本年累计超支的预算科目（元） */
    public record BudgetOverrun(String dept, String accountCode, String accountName, BigDecimal budgetYtd, BigDecimal actualYtd,
                                BigDecimal overAmount) {
    }
}
