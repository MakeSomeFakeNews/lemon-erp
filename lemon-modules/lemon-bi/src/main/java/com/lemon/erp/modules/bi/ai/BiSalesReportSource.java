package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.modules.bi.ai.BiReportPivots.RankItem;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.model.BiCube;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.bi.ai.BiReportPivots.count;
import static com.lemon.erp.modules.bi.ai.BiReportPivots.dec;
import static com.lemon.erp.modules.bi.ai.BiReportPivots.ratio;

/**
 * 智能报告「销售」：数据仓库的销售出库（多维分析的销售数据集），本期、上期、去年同期的销售额、成本、毛利、毛利率，
 * 客户和物料分类排行。按数据权限过滤（与多维分析相同），不要求全部数据
 */
@Component
@RequiredArgsConstructor
public class BiSalesReportSource implements AiReportSource {

    private final BiReportPivots pivots;

    @Override
    public String key() {
        return "bi.sales";
    }

    @Override
    public String labelKey() {
        return "bi.ai.report.sales";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public String permission() {
        return "bi:olap:list";
    }

    @Override
    public SalesSnapshot snapshot(AiReportPeriod period) {
        PivotResult byCustomer = pivots.pivot(BiCube.SALES, "customer", period.start(), period.end());
        SalesTotals current = totals(byCustomer.total());
        AiReportPeriod prev = period.previous();
        SalesTotals previous = totals(pivots.total(BiCube.SALES, prev.start(), prev.end()));
        SalesTotals lastYear = totals(pivots.total(BiCube.SALES, period.start().minusYears(1), period.end().minusYears(1)));
        PivotResult byCategory = pivots.pivot(BiCube.SALES, "category", period.start(), period.end());
        return new SalesSnapshot(pivots.syncedAt(), current, previous, lastYear,
                AiReportMetrics.growth(current.amount(), previous.amount()),
                AiReportMetrics.growth(current.amount(), lastYear.amount()),
                AiReportMetrics.growth(current.profit(), previous.profit()),
                current.marginPct() == null || previous.marginPct() == null ? null : current.marginPct().subtract(previous.marginPct()),
                pivots.top(byCustomer, "amount", "profit", null),
                pivots.top(byCategory, "amount", "profit", "wms_material_category"));
    }

    static SalesTotals totals(Map<String, Object> t) {
        return new SalesTotals(dec(t, "amount"), dec(t, "cost"), dec(t, "profit"), ratio(t, "margin"), dec(t, "qty"),
                count(t, "docs"), count(t, "customers"));
    }

    /**
     * 销售期间汇总
     *
     * @param amount       销售额（元，不含税）
     * @param cost         销售成本（元）
     * @param profit       毛利（元）
     * @param marginPct    毛利率（%）
     * @param qty          数量
     * @param outboundDocs 出库单数
     * @param customers    成交客户数
     */
    public record SalesTotals(BigDecimal amount, BigDecimal cost, BigDecimal profit, BigDecimal marginPct, BigDecimal qty,
                              long outboundDocs, long customers) {
    }

    /**
     * 销售快照
     *
     * @param dataSyncedAt         数据仓库最近同步时间（数据截至此时）
     * @param amountChangePct      销售额环比（%）
     * @param amountYoyPct         销售额同比（%）
     * @param profitChangePct      毛利环比（%）
     * @param marginChangePoints   毛利率比上期变化（百分点）
     * @param topCustomers         销售额前 5 的客户
     * @param topCategories        销售额前 5 的物料分类
     */
    public record SalesSnapshot(LocalDateTime dataSyncedAt, SalesTotals current, SalesTotals previous, SalesTotals sameLastYear,
                                BigDecimal amountChangePct, BigDecimal amountYoyPct, BigDecimal profitChangePct,
                                BigDecimal marginChangePoints, List<RankItem> topCustomers, List<RankItem> topCategories) {
    }
}
