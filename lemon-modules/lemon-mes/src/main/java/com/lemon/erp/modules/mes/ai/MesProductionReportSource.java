package com.lemon.erp.modules.mes.ai;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.modules.mes.service.MesProductionSummaryService;
import com.lemon.erp.modules.mes.service.MesProductionSummaryService.PeriodSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 智能报告「生产」：本期与上期的工单下达 / 完工 / 按期完工率、报工产量与报废率、工时，当前在制与延期工单。
 * 汇总全公司数据，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class MesProductionReportSource implements AiReportSource {

    private final MesProductionSummaryService summaryService;

    @Override
    public String key() {
        return "mes.production";
    }

    @Override
    public String labelKey() {
        return "mes.ai.report.production";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public String permission() {
        return "mes:wo:list";
    }

    @Override
    public boolean requiresAllData() {
        return true;
    }

    @Override
    public ProductionSnapshot snapshot(AiReportPeriod period) {
        PeriodSummary current = summaryService.summary(period.start(), period.end());
        AiReportPeriod prev = period.previous();
        PeriodSummary previous = summaryService.summary(prev.start(), prev.end());
        return new ProductionSnapshot(current, previous,
                AiReportMetrics.growth(current.goodQty(), previous.goodQty()),
                AiReportMetrics.growth(current.completedOrders(), previous.completedOrders()),
                summaryService.openOrders(), summaryService.lateOrders());
    }

    /**
     * 生产快照
     *
     * @param goodQtyChangePct         报工合格数量环比（%）
     * @param completedOrdersChangePct 完工工单数环比（%）
     * @param openOrdersNow            当前在制工单数
     * @param lateOrdersNow            当前延期工单数（在制且已过计划完工日期）
     */
    public record ProductionSnapshot(PeriodSummary current, PeriodSummary previous, BigDecimal goodQtyChangePct,
                                     BigDecimal completedOrdersChangePct, long openOrdersNow, long lateOrdersNow) {
    }
}
