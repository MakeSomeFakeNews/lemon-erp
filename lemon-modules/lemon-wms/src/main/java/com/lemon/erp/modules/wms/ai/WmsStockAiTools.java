package com.lemon.erp.modules.wms.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.wms.dto.BatchQuery;
import com.lemon.erp.modules.wms.dto.BatchView;
import com.lemon.erp.modules.wms.dto.FlowQuery;
import com.lemon.erp.modules.wms.dto.InOutQuery;
import com.lemon.erp.modules.wms.dto.InOutRow;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import com.lemon.erp.modules.wms.dto.StockQuery;
import com.lemon.erp.modules.wms.dto.StockSummaryView;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.lemon.erp.modules.wms.model.StockAlert;
import com.lemon.erp.modules.wms.model.StockAlertMode;
import java.util.Map;

import static com.lemon.erp.modules.wms.ai.AiQuerySupport.LIMIT;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.END_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.START_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.limit;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.nz;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.plain;

/**
 * 仓库模块的 AI 问数工具：库存、低于安全库存、临期批次、库龄、收发存汇总、出入库流水。
 * 全部调用 {@link WmsStockQueryService} 的查询方法（与对应页面相同的数据），权限为对应页面的查看权限
 */
public final class WmsStockAiTools {

    private static final String KEYWORD = "keyword";
    private static final String WAREHOUSE = "warehouse";
    private static final String CATEGORY = "category";
    /** 在 Java 中排序的结果最多读取的批次数 */
    private static final int MAX_BATCHES = 2000;

    private WmsStockAiTools() {
    }

    private static AiSchema keyword() {
        return AiSchema.string("wms.ai.param.materialKeyword").maxLength(64);
    }

    private static AiSchema warehouse() {
        return AiSchema.string("wms.ai.param.warehouse").maxLength(64);
    }

    private static AiSchema category() {
        return AiSchema.string("wms.ai.param.category").maxLength(32);
    }

    // ---------------------------------------------------------------- 库存

    /**
     * 按物料汇总的库存：availableQty = qty - frozenQty；safetyStock、maxStock 为库存下限、上限，
     * belowSafety、aboveMax 为是否低于下限 / 高于上限（分仓库预警时按仓库判断，任一仓库低于 / 高于即为是）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockRow(String materialCode, String materialName, String spec, String unit, String category,
                           BigDecimal qty, BigDecimal frozenQty, BigDecimal availableQty, BigDecimal safetyStock,
                           BigDecimal maxStock, StockAlertMode alertMode, Boolean belowSafety, Boolean aboveMax) {
    }

    /** 库存明细（仓库 + 库位 + 批次） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockDetailRow(String warehouse, String location, String materialCode, String materialName, String spec,
                                 String unit, String batchNo, LocalDate expiryDate, BigDecimal qty, BigDecimal frozenQty) {
    }

    @Component
    @RequiredArgsConstructor
    public static class StockTool implements AiTool {

        private final WmsStockQueryService queryService;
        private final WmsAiLookup lookup;

        public String name() {
            return "wms_stock";
        }

        public String labelKey() {
            return "wms.ai.tool.stock";
        }

        public String descriptionKey() {
            return "wms.ai.tool.stock.desc";
        }

        public String permission() {
            return "wms:stock:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(KEYWORD, keyword(), false)
                    .prop(WAREHOUSE, warehouse(), false)
                    .prop(CATEGORY, category(), false)
                    .prop("lowStockOnly", AiSchema.bool("wms.ai.param.lowStockOnly"), false)
                    .prop("highStockOnly", AiSchema.bool("wms.ai.param.highStockOnly"), false)
                    .prop("detail", AiSchema.bool("wms.ai.param.stockDetail"), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            Long warehouseId = lookup.warehouseId(args.getString(WAREHOUSE));
            boolean lowStock = Boolean.TRUE.equals(args.getBoolean("lowStockOnly"));
            boolean highStock = Boolean.TRUE.equals(args.getBoolean("highStockOnly"));
            int limit = limit(args);
            if (Boolean.TRUE.equals(args.getBoolean("detail")) && !lowStock && !highStock) {
                PageResult<StockView> page = queryService.stockPage(1, limit, new StockQuery(warehouseId, null, null,
                        args.getString(KEYWORD), null, args.getString(CATEGORY), false, null, null));
                return AiToolResult.rows(page.records().stream().map(s -> new StockDetailRow(s.getWarehouseName(),
                        s.getLocationCode(), s.getMaterialCode(), s.getMaterialName(), s.getSpec(), s.getUnit(),
                        s.getBatchNo() == null || s.getBatchNo().isEmpty() ? null : s.getBatchNo(), s.getExpiryDate(),
                        plain(s.getQty()), plain(s.getFrozenQty()))).toList(), page.total());
            }
            PageResult<StockSummaryView> page = queryService.summaryPage(1, limit, new StockQuery(warehouseId, null, null,
                    args.getString(KEYWORD), null, args.getString(CATEGORY), false, lowStock, highStock));
            return AiToolResult.rows(page.records().stream().map(WmsStockAiTools::stockRow).toList(), page.total());
        }
    }

    static StockRow stockRow(StockSummaryView s) {
        BigDecimal qty = nz(s.getQty());
        Boolean below = s.getSafetyStock() == null ? null : s.getStockAlert() == StockAlert.LOW;
        Boolean above = s.getMaxStock() == null ? null : s.getStockAlert() == StockAlert.HIGH
                || s.getHighWarehouses() != null && s.getHighWarehouses() > 0;
        return new StockRow(s.getMaterialCode(), s.getMaterialName(), s.getSpec(), s.getUnit(), s.getCategory(),
                plain(qty), plain(nz(s.getFrozenQty())), plain(qty.subtract(nz(s.getFrozenQty()))), plain(s.getSafetyStock()),
                plain(s.getMaxStock()), s.getSafetyStock() == null && s.getMaxStock() == null ? null : s.getAlertMode(), below, above);
    }

    // ---------------------------------------------------------------- 临期批次

    /** 临期 / 过期批次：daysLeft 为距有效期的天数，负数表示已过期 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ExpiringBatchRow(String materialCode, String materialName, String spec, String unit, String batchNo,
                                   LocalDate productionDate, LocalDate expiryDate, long daysLeft, BigDecimal qty) {
    }

    @Component
    @RequiredArgsConstructor
    public static class ExpiringBatchTool implements AiTool {

        private final WmsStockQueryService queryService;

        public String name() {
            return "wms_expiring_batches";
        }

        public String labelKey() {
            return "wms.ai.tool.expiringBatches";
        }

        public String descriptionKey() {
            return "wms.ai.tool.expiringBatches.desc";
        }

        public String permission() {
            return "wms:trace:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("days", AiSchema.integer("wms.ai.param.expiryDays").min(0).max(3650), false)
                    .prop("includeExpired", AiSchema.bool("wms.ai.param.includeExpired"), false)
                    .prop(KEYWORD, keyword(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            LocalDate today = LocalDate.now();
            int days = args.getInt("days", 30);
            boolean includeExpired = !Boolean.FALSE.equals(args.getBoolean("includeExpired"));
            BatchQuery query = new BatchQuery(null, args.getString(KEYWORD), null, today.plusDays(days), true);
            List<ExpiringBatchRow> rows = AiQuerySupport.fetch((c, s) -> queryService.batchPage(c, s, query), MAX_BATCHES).stream()
                    .filter(b -> b.getExpiryDate() != null && (includeExpired || !b.getExpiryDate().isBefore(today)))
                    .sorted(Comparator.comparing(BatchView::getExpiryDate))
                    .map(b -> new ExpiringBatchRow(b.getMaterialCode(), b.getMaterialName(), b.getSpec(), b.getUnit(), b.getBatchNo(),
                            b.getProductionDate(), b.getExpiryDate(), ChronoUnit.DAYS.between(today, b.getExpiryDate()), plain(b.getQty())))
                    .toList();
            return AiToolResult.rows(rows.subList(0, Math.min(rows.size(), limit(args))), (long) rows.size());
        }
    }

    // ---------------------------------------------------------------- 库龄

    /** 批次库龄：inDate 为批次首次入库日期，ageDays 为至今的天数，ageBucket 为区间（天） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StockAgeRow(String materialCode, String materialName, String spec, String unit, String batchNo,
                              LocalDate inDate, long ageDays, String ageBucket, BigDecimal qty) {
    }

    /** 库龄区间的上限（天），超过最后一个为「365+」 */
    private static final int[] AGE_BUCKETS = {30, 90, 180, 365};

    static String ageBucket(long days) {
        int lower = 0;
        for (int upper : AGE_BUCKETS) {
            if (days <= upper) {
                return lower + "-" + upper;
            }
            lower = upper + 1;
        }
        return AGE_BUCKETS[AGE_BUCKETS.length - 1] + "+";
    }

    @Component
    @RequiredArgsConstructor
    public static class StockAgeTool implements AiTool {

        private final WmsStockQueryService queryService;

        public String name() {
            return "wms_stock_age";
        }

        public String labelKey() {
            return "wms.ai.tool.stockAge";
        }

        public String descriptionKey() {
            return "wms.ai.tool.stockAge.desc";
        }

        public String permission() {
            return "wms:stock:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("minDays", AiSchema.integer("wms.ai.param.minAgeDays").min(0).max(3650), false)
                    .prop(KEYWORD, keyword(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            LocalDate today = LocalDate.now();
            int minDays = args.getInt("minDays", 0);
            BatchQuery query = new BatchQuery(null, args.getString(KEYWORD), null, null, true);
            List<StockAgeRow> rows = AiQuerySupport.fetch((c, s) -> queryService.batchPage(c, s, query), MAX_BATCHES).stream()
                    .filter(b -> b.getCreateTime() != null && nz(b.getQty()).signum() > 0)
                    .map(b -> {
                        LocalDate in = b.getCreateTime().toLocalDate();
                        long age = ChronoUnit.DAYS.between(in, today);
                        return new StockAgeRow(b.getMaterialCode(), b.getMaterialName(), b.getSpec(), b.getUnit(), b.getBatchNo(),
                                in, age, ageBucket(age), plain(b.getQty()));
                    })
                    .filter(r -> r.ageDays() >= minDays)
                    .sorted(Comparator.comparingLong(StockAgeRow::ageDays).reversed())
                    .toList();
            return AiToolResult.rows(rows.subList(0, Math.min(rows.size(), limit(args))), (long) rows.size());
        }
    }

    // ---------------------------------------------------------------- 收发存汇总

    /** 收发存汇总的一行（数量，按基本单位）：openQty + 各类入库 - 各类出库 = closeQty */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InOutSummaryRow(String materialCode, String materialName, String spec, String unit, BigDecimal openQty,
                                  BigDecimal purchaseIn, BigDecimal productionIn, BigDecimal salesReturn, BigDecimal otherIn,
                                  BigDecimal salesOut, BigDecimal productionOut, BigDecimal purchaseReturn, BigDecimal otherOut,
                                  BigDecimal closeQty) {

        static InOutSummaryRow of(InOutRow r) {
            return new InOutSummaryRow(r.getMaterialCode(), r.getMaterialName(), r.getSpec(), r.getUnit(), plain(r.getOpenQty()),
                    plain(r.getPurchaseIn()), plain(r.getProductionIn()), plain(r.getSalesReturn()), plain(r.getOtherIn()),
                    plain(r.getSalesOut()), plain(r.getProductionOut()), plain(r.getPurchaseReturn()), plain(r.getOtherOut()),
                    plain(r.getCloseQty()));
        }
    }

    @Component
    @RequiredArgsConstructor
    public static class InOutSummaryTool implements AiTool {

        private final WmsStockQueryService queryService;
        private final WmsAiLookup lookup;

        public String name() {
            return "wms_in_out_summary";
        }

        public String labelKey() {
            return "wms.ai.tool.inOutSummary";
        }

        public String descriptionKey() {
            return "wms.ai.tool.inOutSummary.desc";
        }

        public String permission() {
            return "wms:inout:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(KEYWORD, keyword(), false)
                    .prop(WAREHOUSE, warehouse(), false)
                    .prop(CATEGORY, category(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            AiQuerySupport.Range range = AiQuerySupport.monthToDate(args);
            PageResult<InOutRow> page = queryService.inOutPage(1, limit(args), new InOutQuery(range.begin(), range.end(),
                    lookup.warehouseId(args.getString(WAREHOUSE)), args.getString(CATEGORY), args.getString(KEYWORD)));
            return AiToolResult.rows(page.records().stream().map(InOutSummaryRow::of).toList(), page.total());
        }
    }

    // ---------------------------------------------------------------- 出入库流水

    /** 库存流水：qty 为正表示增加（入库、调入、盘盈），为负表示减少 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FlowRow(@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time, StockBizType bizType, String docNo, String warehouse, String materialCode,
                          String materialName, String unit, String batchNo, BigDecimal qty, String operator) {
    }

    @Component
    @RequiredArgsConstructor
    public static class StockFlowTool implements AiTool {

        private final WmsStockQueryService queryService;
        private final WmsAiLookup lookup;

        public String name() {
            return "wms_stock_flow";
        }

        public String labelKey() {
            return "wms.ai.tool.stockFlow";
        }

        public String descriptionKey() {
            return "wms.ai.tool.stockFlow.desc";
        }

        public String permission() {
            return "wms:flow:list";
        }

        public Map<String, Object> parameters() {
            String[] types = Arrays.stream(StockBizType.values()).map(Enum::name).toArray(String[]::new);
            return AiSchema.object()
                    .prop(KEYWORD, keyword(), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop("bizType", AiSchema.enumOf("wms.ai.param.flowBizType", types), false)
                    .prop(WAREHOUSE, warehouse(), false)
                    .prop("batchNo", AiSchema.string("wms.ai.param.batchNo").maxLength(64), false)
                    .prop("docNo", AiSchema.string("wms.ai.param.docNo").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            AiQuerySupport.Range range = AiQuerySupport.range(args, end -> end.minusDays(29));
            PageResult<StockFlowView> page = queryService.flowPage(1, limit(args), new FlowQuery(args.getString("bizType"),
                    lookup.warehouseId(args.getString(WAREHOUSE)), null, args.getString(KEYWORD), args.getString("batchNo"),
                    args.getString("docNo"), range.begin(), range.end()));
            return AiToolResult.rows(page.records().stream().map(f -> new FlowRow(f.getCreateTime(), f.getBizType(), f.getDocNo(),
                    f.getWarehouseName(), f.getMaterialCode(), f.getMaterialName(), f.getUnit(),
                    f.getBatchNo() == null || f.getBatchNo().isEmpty() ? null : f.getBatchNo(), plain(f.getQty()),
                    f.getCreateByName())).toList(), page.total());
        }
    }
}
