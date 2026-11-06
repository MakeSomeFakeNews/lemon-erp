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

/**
 * 智能报告「采购」：数据仓库的采购入库（多维分析的采购数据集），本期、上期、去年同期的采购额，供应商和物料分类排行。
 * 采购数据不按部门区分，汇总的是全公司数据，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class BiPurchaseReportSource implements AiReportSource {

    private final BiReportPivots pivots;

    @Override
    public String key() {
        return "bi.purchase";
    }

    @Override
    public String labelKey() {
        return "bi.ai.report.purchase";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public String permission() {
        return "bi:olap:list";
    }

    @Override
    public boolean requiresAllData() {
        return true;
    }

    @Override
    public PurchaseSnapshot snapshot(AiReportPeriod period) {
        PivotResult bySupplier = pivots.pivot(BiCube.PURCHASE, "supplier", period.start(), period.end());
        PurchaseTotals current = totals(bySupplier.total());
        AiReportPeriod prev = period.previous();
        PurchaseTotals previous = totals(pivots.total(BiCube.PURCHASE, prev.start(), prev.end()));
        PurchaseTotals lastYear = totals(pivots.total(BiCube.PURCHASE, period.start().minusYears(1), period.end().minusYears(1)));
        PivotResult byCategory = pivots.pivot(BiCube.PURCHASE, "category", period.start(), period.end());
        return new PurchaseSnapshot(pivots.syncedAt(), current, previous, lastYear,
                AiReportMetrics.growth(current.amount(), previous.amount()),
                AiReportMetrics.growth(current.amount(), lastYear.amount()),
                pivots.top(bySupplier, "amount", null, null),
                pivots.top(byCategory, "amount", null, "wms_material_category"));
    }

    static PurchaseTotals totals(Map<String, Object> t) {
        return new PurchaseTotals(dec(t, "amount"), dec(t, "qty"), count(t, "docs"), count(t, "suppliers"));
    }

    /**
     * 采购期间汇总
     *
     * @param amount      采购入库金额（元）
     * @param qty         数量
     * @param inboundDocs 入库单数
     * @param suppliers   供应商数
     */
    public record PurchaseTotals(BigDecimal amount, BigDecimal qty, long inboundDocs, long suppliers) {
    }

    /**
     * 采购快照
     *
     * @param amountChangePct 采购额环比（%）
     * @param amountYoyPct    采购额同比（%）
     * @param topSuppliers    采购额前 5 的供应商
     * @param topCategories   采购额前 5 的物料分类
     */
    public record PurchaseSnapshot(LocalDateTime dataSyncedAt, PurchaseTotals current, PurchaseTotals previous,
                                   PurchaseTotals sameLastYear, BigDecimal amountChangePct, BigDecimal amountYoyPct,
                                   List<RankItem> topSuppliers, List<RankItem> topCategories) {
    }
}
