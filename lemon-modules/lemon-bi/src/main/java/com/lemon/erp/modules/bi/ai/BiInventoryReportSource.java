package com.lemon.erp.modules.bi.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.modules.bi.ai.BiReportPivots.RankItem;
import com.lemon.erp.modules.bi.dto.BiInsight;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.model.BiAlertMetric;
import com.lemon.erp.modules.bi.model.BiCube;
import com.lemon.erp.modules.bi.service.BiDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.bi.ai.BiReportPivots.count;
import static com.lemon.erp.modules.bi.ai.BiReportPivots.dec;

/**
 * 智能报告「库存」：期末与上期末的库存快照（多维分析的库存数据集，取期间内最后一次快照）、按物料分类的库存金额，
 * 以及当前低于安全库存的物料数和呆滞物料（决策支持）。库存不按部门区分，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class BiInventoryReportSource implements AiReportSource {

    private final BiReportPivots pivots;
    private final BiDecisionService decisionService;

    @Override
    public String key() {
        return "bi.inventory";
    }

    @Override
    public String labelKey() {
        return "bi.ai.report.inventory";
    }

    @Override
    public int order() {
        return 30;
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
    public InventorySnapshot snapshot(AiReportPeriod period) {
        StockLevel end = level(period);
        StockLevel previous = level(period.previous());
        List<RankItem> byCategory = end == null ? List.of()
                : pivots.top(pivots.pivot(BiCube.INVENTORY, "category", end.snapshotDate(), end.snapshotDate()), "amount", null,
                "wms_material_category");
        List<BiInsight.SlowMovingRow> slow = decisionService.slowMoving(BiDecisionService.SLOW_DAYS);
        SlowMoving slowMoving = new SlowMoving(BiDecisionService.SLOW_DAYS, slow.size(), Decimals.sum(slow, r -> Decimals.nz(r.getAmount())),
                slow.stream().limit(BiReportPivots.TOP)
                        .map(r -> new SlowItem(r.getCode(), r.getName(), r.getQty(), r.getAmount(), r.getIdleDays())).toList());
        BigDecimal below = decisionService.currentValue(BiAlertMetric.STOCK_BELOW_SAFETY);
        return new InventorySnapshot(pivots.syncedAt(), end, previous,
                end == null || previous == null ? null : AiReportMetrics.growth(end.value(), previous.value()),
                byCategory, below == null ? 0 : below.longValue(), slowMoving);
    }

    /** 期间内最后一次库存快照的合计，期间内没有快照时为 null */
    private StockLevel level(AiReportPeriod period) {
        PivotResult byDate = pivots.pivot(BiCube.INVENTORY, "snapDate", period.start(), period.end());
        Map<String, Object> last = null;
        for (Map<String, Object> row : byDate.rowTotals()) {
            if (last == null || String.valueOf(row.get("r")).compareTo(String.valueOf(last.get("r"))) > 0) {
                last = row;
            }
        }
        if (last == null) {
            return null;
        }
        return new StockLevel(LocalDate.parse(String.valueOf(last.get("r")).substring(0, 10)), dec(last, "qty"), dec(last, "amount"),
                count(last, "materials"));
    }

    /**
     * 某天的库存快照合计
     *
     * @param value 库存金额（元）
     */
    public record StockLevel(LocalDate snapshotDate, BigDecimal qty, BigDecimal value, long materials) {
    }

    /**
     * 呆滞物料（当前）：有库存但超过 idleDaysThreshold 天没有出库
     *
     * @param amount 呆滞库存金额（元）
     * @param top    金额最大的 5 种
     */
    public record SlowMoving(int idleDaysThreshold, long materials, BigDecimal amount, List<SlowItem> top) {
    }

    public record SlowItem(String code, String name, BigDecimal qty, BigDecimal amount, Long idleDays) {
    }

    /**
     * 库存快照
     *
     * @param periodEnd            期末（期间内最后一次快照），没有快照时为空
     * @param previousPeriodEnd    上期末
     * @param valueChangePct       库存金额比上期末的变化（%）
     * @param byCategory           期末库存金额前 5 的物料分类
     * @param belowSafetyStockNow  当前低于安全库存的物料数
     * @param slowMovingNow        当前的呆滞物料
     */
    public record InventorySnapshot(LocalDateTime dataSyncedAt, StockLevel periodEnd, StockLevel previousPeriodEnd,
                                    BigDecimal valueChangePct, List<RankItem> byCategory, long belowSafetyStockNow,
                                    SlowMoving slowMovingNow) {
    }
}
