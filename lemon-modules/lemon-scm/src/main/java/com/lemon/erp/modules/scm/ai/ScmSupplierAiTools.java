package com.lemon.erp.modules.scm.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.scm.dto.ScmPriceViews;
import com.lemon.erp.modules.scm.dto.SupplierKpi;
import com.lemon.erp.modules.scm.model.SupplierLevel;
import com.lemon.erp.modules.scm.service.ScmSupplierPriceService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.ai.AiQuerySupport;
import com.lemon.erp.modules.wms.ai.WmsAiLookup;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.lemon.erp.modules.wms.ai.AiQuerySupport.END_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.LIMIT;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.START_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.limit;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.plain;

/**
 * 供应链模块的 AI 问数工具：采购价格历史、供应商绩效（准时交货率、来料合格率、退货率等，可按指标排行）
 */
public final class ScmSupplierAiTools {

    /** 绩效排行时最多读取的供应商数 */
    private static final int MAX_SUPPLIERS = 2000;

    private ScmSupplierAiTools() {
    }

    // ---------------------------------------------------------------- 采购价格历史

    /** 采购入库的成交价：price 为含税单价（本位币），taxRate 为税率（%） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PriceRow(LocalDate bizDate, String docNo, String supplierName, String materialCode, String materialName,
                           String unit, BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
    }

    @Component
    @RequiredArgsConstructor
    public static class PriceHistoryTool implements AiTool {

        private final ScmSupplierPriceService priceService;
        private final WmsAiLookup wmsLookup;
        private final ScmAiLookup lookup;

        public String name() {
            return "scm_purchase_price_history";
        }

        public String labelKey() {
            return "scm.ai.tool.priceHistory";
        }

        public String descriptionKey() {
            return "scm.ai.tool.priceHistory.desc";
        }

        public String permission() {
            return "scm:purchase:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("material", AiSchema.string("scm.ai.param.materialExact").maxLength(64), true)
                    .prop("supplier", AiSchema.string("scm.ai.param.supplier").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            WmsMaterial m = wmsLookup.material(args.getString("material"));
            List<ScmPriceViews.PurchaseHistory> history = priceService.history(m.getId(), lookup.supplierId(args.getString("supplier")));
            List<PriceRow> rows = history.stream().limit(limit(args)).map(h -> new PriceRow(h.getBizDate(), h.getDocNo(),
                    h.getSupplierName(), m.getCode(), m.getName(), m.getUnit(), plain(h.getQty()), plain(h.getPrice()),
                    plain(h.getTaxRate()))).toList();
            return AiToolResult.rows(rows, (long) history.size());
        }
    }

    // ---------------------------------------------------------------- 供应商绩效

    /**
     * 供应商绩效：purchaseAmount 为采购入库金额（本位币，扣除退货），onTimeRate 准时交货率、iqcPassRate 来料批次合格率、
     * returnRate 退货率均为百分数；lastScore / lastGrade 为最近一次评估
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SupplierRow(String supplierCode, String supplierName, SupplierLevel level, Long orderCount,
                              BigDecimal purchaseAmount, BigDecimal onTimeRate, Long iqcBatches, BigDecimal iqcPassRate,
                              BigDecimal returnRate, Long claimCount, BigDecimal claimAmount, BigDecimal lastScore,
                              SupplierLevel lastGrade) {
    }

    /** 排行指标 → 取值（从高到低排列，没有数据的排在最后） */
    private static final Map<String, Function<SupplierKpi, BigDecimal>> SORTS = Map.of(
            "PURCHASE_AMOUNT", SupplierKpi::getPurchaseAmount,
            "ON_TIME_RATE", SupplierKpi::getOnTimeRate,
            "IQC_PASS_RATE", SupplierKpi::getIqcPassRate,
            "RETURN_RATE", SupplierKpi::getReturnRate);

    @Component
    @RequiredArgsConstructor
    public static class SupplierPerformanceTool implements AiTool {

        private final ScmSupplierService supplierService;

        public String name() {
            return "scm_supplier_performance";
        }

        public String labelKey() {
            return "scm.ai.tool.supplierPerformance";
        }

        public String descriptionKey() {
            return "scm.ai.tool.supplierPerformance.desc";
        }

        public String permission() {
            return "scm:supplier:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop("supplier", AiSchema.string("scm.ai.param.supplier").maxLength(64), false)
                    .prop("sortBy", AiSchema.enumOf("scm.ai.param.supplierSort", "PURCHASE_AMOUNT", "ON_TIME_RATE",
                            "IQC_PASS_RATE", "RETURN_RATE"), false)
                    .prop("ascending", AiSchema.bool("scm.ai.param.ascending"), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            AiQuerySupport.Range range = AiQuerySupport.range(args, end -> end.minusYears(1).plusDays(1));
            String sortBy = args.getString("sortBy");
            Function<SupplierKpi, BigDecimal> metric = SORTS.get(sortBy == null ? "PURCHASE_AMOUNT" : sortBy);
            Comparator<BigDecimal> order = Boolean.TRUE.equals(args.getBoolean("ascending"))
                    ? Comparator.naturalOrder() : Comparator.reverseOrder();
            List<SupplierKpi> all = AiQuerySupport.fetch((c, s) -> supplierService.kpi(c, s, args.getString("supplier"),
                    range.begin(), range.end()), MAX_SUPPLIERS);
            List<SupplierRow> rows = all.stream()
                    .sorted(Comparator.comparing(metric, Comparator.nullsLast(order)))
                    .limit(limit(args))
                    .map(k -> new SupplierRow(k.getSupplierCode(), k.getSupplierName(), k.getLevel(), k.getOrderCount(),
                            k.getPurchaseAmount(), k.getOnTimeRate(), k.getIqcBatches(), k.getIqcPassRate(), k.getReturnRate(),
                            k.getClaimCount(), k.getClaimAmount(), k.getLastScore(), k.getLastGrade()))
                    .toList();
            return AiToolResult.rows(rows, (long) all.size());
        }
    }
}
