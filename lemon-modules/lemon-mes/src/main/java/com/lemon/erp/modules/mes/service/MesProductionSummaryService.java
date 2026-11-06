package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.modules.mes.mapper.MesProductionSummaryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产期间汇总（智能报告的生产来源使用）：期间内下达 / 完工 / 按期完工的工单，报工的合格、报废数量和工时，
 * 当前在制与延期的工单。汇总全公司数据，不按数据权限过滤
 */
@Service
@RequiredArgsConstructor
public class MesProductionSummaryService {

    private final MesProductionSummaryMapper summaryMapper;

    /** 期间（含首尾两天）的汇总 */
    public PeriodSummary summary(LocalDate begin, LocalDate end) {
        OrderCounts o = summaryMapper.selectOrderCounts(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
        if (o == null) {
            o = new OrderCounts();
        }
        ReportTotals r = summaryMapper.selectReportTotals(begin, end);
        if (r == null) {
            r = new ReportTotals();
        }
        BigDecimal good = AiReportMetrics.nz(r.getGoodQty());
        BigDecimal scrap = AiReportMetrics.nz(r.getScrapQty());
        return new PeriodSummary(o.getReleased(), o.getCompleted(), o.getCompletedOnTime(),
                AiReportMetrics.percent(o.getCompletedOnTime(), o.getCompleted()),
                AiReportMetrics.nz(o.getCompletedPlanQty()), AiReportMetrics.nz(o.getCompletedFinishedQty()),
                r.getReports(), good, scrap, AiReportMetrics.percent(scrap, good.add(scrap)), AiReportMetrics.nz(r.getHours()));
    }

    /** 当前在制（已下达、生产中）的工单数 */
    public long openOrders() {
        return summaryMapper.selectOpenOrders();
    }

    /** 当前延期（在制且计划完工日期已过）的工单数 */
    public long lateOrders() {
        return summaryMapper.selectLateOrders(LocalDate.now());
    }

    /**
     * 期间汇总
     *
     * @param releasedOrders       下达的工单数
     * @param completedOrders      完工的工单数
     * @param completedOnTime      其中在计划完工日期（含）之前完工的
     * @param onTimeRatePct        按期完工率（%）
     * @param completedPlanQty     完工工单的计划数量
     * @param completedFinishedQty 完工工单的完工入库数量
     * @param reports              报工笔数
     * @param goodQty              报工合格数量
     * @param scrapQty             报工报废数量
     * @param scrapRatePct         报废率（%）= 报废 / (合格 + 报废)
     * @param hours                报工工时（小时）
     */
    public record PeriodSummary(long releasedOrders, long completedOrders, long completedOnTime, BigDecimal onTimeRatePct,
                                BigDecimal completedPlanQty, BigDecimal completedFinishedQty, long reports, BigDecimal goodQty,
                                BigDecimal scrapQty, BigDecimal scrapRatePct, BigDecimal hours) {
    }

    /** 工单计数（数据库查询结果） */
    @Data
    public static class OrderCounts {
        private long released;
        private long completed;
        private long completedOnTime;
        private BigDecimal completedPlanQty;
        private BigDecimal completedFinishedQty;
    }

    /** 报工合计（数据库查询结果） */
    @Data
    public static class ReportTotals {
        private long reports;
        private BigDecimal goodQty;
        private BigDecimal scrapQty;
        private BigDecimal hours;
    }
}
