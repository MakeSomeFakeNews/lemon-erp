package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.model.AlertLevel;
import com.lemon.erp.modules.bi.service.BiCockpitService;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 经营驾驶舱关键指标与经营预警：复用 {@link BiCockpitService#cockpit} 与 {@link BiDecisionService#alerts}，
 * 需要全部数据权限（汇总全公司数据，与驾驶舱页面一致）
 */
@Component
@RequiredArgsConstructor
public class BiCockpitTool implements AiTool {

    /** 近几个月的趋势 */
    private static final int TREND_MONTHS = 6;
    private static final int TOP = 5;

    /** 触发中的预警 */
    record Alert(String name, String metric, String unit, BigDecimal value, String operator, BigDecimal threshold, AlertLevel level) {
    }

    /** 客户排行：本年销售额与毛利，金额单位元 */
    record TopCustomer(String name, BigDecimal amount, BigDecimal profit) {
    }

    /**
     * @param kpi       关键指标（销售额、同比环比 %、毛利率 %、应收应付、在制工单、项目、在职人数、商机、工单等）
     * @param trend     近 6 个月的销售额、毛利、采购额、收入、费用、利润（period 为 yyyyMM）
     * @param lastSync  数据仓库最近同步时间（销售趋势等来自数据仓库）
     */
    record Cockpit(BiInsight.CockpitKpi kpi, List<BiInsight.CockpitTrend> trend, List<TopCustomer> topCustomers, List<Alert> triggeredAlerts,
                   LocalDateTime lastSync) {
    }

    private final BiCockpitService cockpitService;
    private final BiDecisionService decisionService;

    @Override
    public String name() {
        return "bi_cockpit";
    }

    @Override
    public String labelKey() {
        return "bi.ai.tool.cockpit";
    }

    @Override
    public String descriptionKey() {
        return "bi.ai.tool.cockpit.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object().build();
    }

    @Override
    public String permission() {
        return "bi:cockpit:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        BiAiSupport.requireAllData();
        BiInsight.Cockpit c = cockpitService.cockpit();
        List<BiInsight.CockpitTrend> trend = c.trend().subList(Math.max(0, c.trend().size() - TREND_MONTHS), c.trend().size());
        List<TopCustomer> customers = c.topCustomers().stream().limit(TOP).map(t -> new TopCustomer(t.getName(), t.getAmount(), t.getProfit())).toList();
        List<Alert> alerts = decisionService.alerts().stream().filter(BiInsight.AlertResult::triggered)
                .map(a -> new Alert(a.name(), a.metricLabel(), a.unit(), a.value(), a.operator() == null ? null : a.operator().name(), a.threshold(), a.level()))
                .toList();
        return AiToolResult.of(new Cockpit(c.kpi(), trend, customers, alerts, c.lastSync()));
    }
}
