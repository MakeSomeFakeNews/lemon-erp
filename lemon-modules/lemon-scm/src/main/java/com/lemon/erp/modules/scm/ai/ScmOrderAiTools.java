package com.lemon.erp.modules.scm.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.scm.dto.BackorderRow;
import com.lemon.erp.modules.scm.dto.OrderQuery;
import com.lemon.erp.modules.scm.dto.OrderSummaryRow;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmOrderStatsService;
import com.lemon.erp.modules.scm.service.ScmOrderStatsService.GroupBy;
import com.lemon.erp.modules.scm.service.ScmPlanningService;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import com.lemon.erp.modules.wms.ai.AiQuerySupport;
import com.lemon.erp.modules.wms.ai.WmsAiLookup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.wms.ai.AiQuerySupport.END_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.LIMIT;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.START_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.limit;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.nz;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.plain;

/**
 * 供应链模块的 AI 问数工具：销售 / 采购订单查询与汇总（含客户、供应商、业务员、物料排行）、欠交（未发货 / 未到货）、
 * 销售出库与退货、采购入库与退货。订单、出入库单都通过本模块 Service 查询，按订单 / 单据的数据权限（制单部门、制单人）过滤
 */
public final class ScmOrderAiTools {

    private static final String PARTNER = "partner";
    private static final String MATERIAL = "material";
    private static final String STATUS = "status";
    private static final String ORDER_NO = "orderNo";
    private static final String WAREHOUSE = "warehouse";
    /** 出入库单合计时最多读取的单据数 */
    private static final int MAX_DOCS = 2000;

    private ScmOrderAiTools() {
    }

    private static AiSchema material() {
        return AiSchema.string("scm.ai.param.material").maxLength(64);
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }

    /** 两个日期都给出时，结束不能早于开始 */
    private static void checkDates(LocalDate begin, LocalDate end) {
        if (begin != null && end != null && end.isBefore(begin)) {
            throw new BizException("wms.error.endDateCannotEarlierStartDate");
        }
    }

    // ---------------------------------------------------------------- 订单查询

    /** 订单：amount 为订单币种的价税合计，doneQty 为已出库 / 已入库数量（扣除退货） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderRow(String orderNo, LocalDate orderDate, String partnerName, String salesman, OrderStatus status,
                           BigDecimal totalQty, BigDecimal doneQty, BigDecimal amount, String currency, LocalDate deliveryDate) {
    }

    abstract static class OrdersTool implements AiTool {

        private final ScmOrderService orderService;

        OrdersTool(ScmOrderService orderService) {
            this.orderService = orderService;
        }

        abstract OrderKind kind();

        abstract String partnerKey();

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(ORDER_NO, AiSchema.string("scm.ai.param.orderNo").maxLength(64), false)
                    .prop(PARTNER, AiSchema.string(partnerKey()).maxLength(64), false)
                    .prop(MATERIAL, material(), false)
                    .prop(STATUS, AiSchema.enumOf("scm.ai.param.orderStatus", names(OrderStatus.values())), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            LocalDate begin = args.getDate(START_DATE);
            LocalDate end = args.getDate(END_DATE);
            checkDates(begin, end);
            String status = args.getString(STATUS);
            OrderQuery query = new OrderQuery(args.getString(ORDER_NO), null, args.getString(PARTNER),
                    status == null ? null : OrderStatus.valueOf(status), args.getString(MATERIAL), begin, end);
            PageResult<ScmOrder> page = orderService.page(kind(), 1, limit(args), query);
            boolean sales = kind() == OrderKind.SALES;
            return AiToolResult.rows(page.records().stream().map(o -> new OrderRow(o.getOrderNo(), o.getOrderDate(),
                    o.getPartnerName(), sales ? o.getSalesmanName() : null, o.getStatus(), plain(o.getTotalQty()),
                    plain(o.getDoneQty()), o.getTotalAmount(), o.getCurrency(), o.getDeliveryDate())).toList(), page.total());
        }
    }

    @Component
    public static class SalesOrdersTool extends OrdersTool {

        public SalesOrdersTool(ScmOrderService orderService) {
            super(orderService);
        }

        OrderKind kind() {
            return OrderKind.SALES;
        }

        String partnerKey() {
            return "scm.ai.param.customer";
        }

        public String name() {
            return "scm_sales_orders";
        }

        public String labelKey() {
            return "scm.ai.tool.salesOrders";
        }

        public String descriptionKey() {
            return "scm.ai.tool.salesOrders.desc";
        }

        public String permission() {
            return "scm:sales:list";
        }
    }

    @Component
    public static class PurchaseOrdersTool extends OrdersTool {

        public PurchaseOrdersTool(ScmOrderService orderService) {
            super(orderService);
        }

        OrderKind kind() {
            return OrderKind.PURCHASE;
        }

        String partnerKey() {
            return "scm.ai.param.supplier";
        }

        public String name() {
            return "scm_purchase_orders";
        }

        public String labelKey() {
            return "scm.ai.tool.purchaseOrders";
        }

        public String descriptionKey() {
            return "scm.ai.tool.purchaseOrders.desc";
        }

        public String permission() {
            return "scm:purchase:list";
        }
    }

    // ---------------------------------------------------------------- 订单汇总

    /**
     * 汇总的一行：rank 为按金额的名次（按月汇总时为空），month 为 yyyy-MM，amount 为本位币价税合计，
     * sharePercent 为占合计的百分比；最后一行为合计（name 为「合计」）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SummaryRow(Integer rank, String code, String name, String month, String unit, Long orderCount, BigDecimal qty,
                             BigDecimal amount, BigDecimal sharePercent) {
    }

    abstract static class SummaryTool implements AiTool {

        private final ScmOrderStatsService statsService;

        SummaryTool(ScmOrderStatsService statsService) {
            this.statsService = statsService;
        }

        abstract OrderKind kind();

        /** 参数 groupBy 的取值 → 汇总维度，第一个为默认 */
        abstract Map<String, GroupBy> groups();

        abstract String partnerKey();

        public Map<String, Object> parameters() {
            AiSchema schema = AiSchema.object()
                    .prop("groupBy", AiSchema.enumOf("scm.ai.param.groupBy", groups().keySet().toArray(String[]::new)), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(PARTNER, AiSchema.string(partnerKey()).maxLength(64), false)
                    .prop(MATERIAL, material(), false);
            if (kind() == OrderKind.SALES) {
                schema.prop("salesman", AiSchema.string("scm.ai.param.salesman").maxLength(64), false);
            }
            return schema.prop(LIMIT, AiQuerySupport.limit(), false).build();
        }

        public Object execute(AiToolArgs args) {
            String key = args.getString("groupBy");
            GroupBy by = key == null ? groups().values().iterator().next() : groups().get(key);
            AiQuerySupport.Range range = AiQuerySupport.monthToDate(args);
            ScmOrderStatsService.SummaryQuery q = new ScmOrderStatsService.SummaryQuery(kind(), by, range.begin(), range.end(),
                    args.getString(PARTNER), args.getString(MATERIAL), kind() == OrderKind.SALES ? args.getString("salesman") : null);
            // 按月最多 13 个月，全部返回
            PageResult<OrderSummaryRow> page = statsService.summary(q, by == GroupBy.MONTH ? AiTool.MAX_ROWS : limit(args));
            OrderSummaryRow total = statsService.total(q);
            BigDecimal totalAmount = Decimals.money(nz(total.getAmount()));
            List<SummaryRow> rows = new ArrayList<>();
            int rank = 0;
            for (OrderSummaryRow r : page.records()) {
                BigDecimal amount = Decimals.money(nz(r.getAmount()));
                boolean month = by == GroupBy.MONTH;
                rows.add(new SummaryRow(month ? null : ++rank, r.getGroupCode(), r.getGroupName(),
                        month ? String.format("%04d-%02d", r.getYearNo(), r.getMonthNo()) : null, r.getUnit(), r.getOrderCount(),
                        by == GroupBy.MATERIAL ? plain(r.getQty()) : null, amount, Decimals.percent(amount, totalAmount, 1)));
            }
            rows.add(new SummaryRow(null, null, Messages.get("scm.ai.text.total"), null, null, total.getOrderCount(),
                    null, totalAmount, totalAmount.signum() == 0 ? null : Decimals.HUNDRED));
            return AiToolResult.rows(rows, page.total() + 1);
        }
    }

    @Component
    public static class SalesSummaryTool extends SummaryTool {

        public SalesSummaryTool(ScmOrderStatsService statsService) {
            super(statsService);
        }

        OrderKind kind() {
            return OrderKind.SALES;
        }

        Map<String, GroupBy> groups() {
            Map<String, GroupBy> map = new LinkedHashMap<>();
            map.put("CUSTOMER", GroupBy.PARTNER);
            map.put("SALESMAN", GroupBy.SALESMAN);
            map.put("MATERIAL", GroupBy.MATERIAL);
            map.put("MONTH", GroupBy.MONTH);
            return map;
        }

        String partnerKey() {
            return "scm.ai.param.customer";
        }

        public String name() {
            return "scm_sales_summary";
        }

        public String labelKey() {
            return "scm.ai.tool.salesSummary";
        }

        public String descriptionKey() {
            return "scm.ai.tool.salesSummary.desc";
        }

        public String permission() {
            return "scm:sales:list";
        }
    }

    @Component
    public static class PurchaseSummaryTool extends SummaryTool {

        public PurchaseSummaryTool(ScmOrderStatsService statsService) {
            super(statsService);
        }

        OrderKind kind() {
            return OrderKind.PURCHASE;
        }

        Map<String, GroupBy> groups() {
            Map<String, GroupBy> map = new LinkedHashMap<>();
            map.put("SUPPLIER", GroupBy.PARTNER);
            map.put("MATERIAL", GroupBy.MATERIAL);
            map.put("MONTH", GroupBy.MONTH);
            return map;
        }

        String partnerKey() {
            return "scm.ai.param.supplier";
        }

        public String name() {
            return "scm_purchase_summary";
        }

        public String labelKey() {
            return "scm.ai.tool.purchaseSummary";
        }

        public String descriptionKey() {
            return "scm.ai.tool.purchaseSummary.desc";
        }

        public String permission() {
            return "scm:purchase:list";
        }
    }

    // ---------------------------------------------------------------- 欠交

    /** 欠交明细：openQty 为未交数量，overdueDays 为已逾期天数（未逾期时为空） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BackorderItem(String orderNo, LocalDate orderDate, String partnerName, String materialCode, String materialName,
                                String spec, String unit, BigDecimal qty, BigDecimal doneQty, BigDecimal openQty,
                                LocalDate deliveryDate, Long overdueDays, BigDecimal availableQty) {
    }

    @Component
    public static class BackordersTool implements AiTool {

        private final ScmPlanningService planningService;
        private final ScmAiLookup lookup;

        public BackordersTool(ScmPlanningService planningService, ScmAiLookup lookup) {
            this.planningService = planningService;
            this.lookup = lookup;
        }

        public String name() {
            return "scm_backorders";
        }

        public String labelKey() {
            return "scm.ai.tool.backorders";
        }

        public String descriptionKey() {
            return "scm.ai.tool.backorders.desc";
        }

        public String permission() {
            return "scm:backorder:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("kind", AiSchema.enumOf("scm.ai.param.backorderKind", names(OrderKind.values())), true)
                    .prop("overdueOnly", AiSchema.bool("scm.ai.param.overdueOnly"), false)
                    .prop(PARTNER, AiSchema.string("scm.ai.param.partner").maxLength(64), false)
                    .prop("keyword", AiSchema.string("scm.ai.param.backorderKeyword").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            OrderKind kind = OrderKind.valueOf(args.getString("kind"));
            LocalDate today = LocalDate.now();
            PageResult<BackorderRow> page = planningService.backorders(1, limit(args), kind,
                    Boolean.TRUE.equals(args.getBoolean("overdueOnly")), args.getString("keyword"),
                    lookup.partnerId(kind, args.getString(PARTNER)));
            return AiToolResult.rows(page.records().stream().map(b -> new BackorderItem(b.getOrderNo(), b.getOrderDate(),
                    b.getPartnerName(), b.getMaterialCode(), b.getMaterialName(), b.getSpec(), b.getUnit(), plain(b.getQty()),
                    plain(b.getDoneQty()), plain(b.getOpenQty()), b.getDeliveryDate(),
                    b.getDeliveryDate() != null && b.getDeliveryDate().isBefore(today) ? ChronoUnit.DAYS.between(b.getDeliveryDate(), today) : null,
                    kind == OrderKind.SALES ? plain(b.getAvailableQty()) : null)).toList(), page.total());
        }
    }

    // ---------------------------------------------------------------- 出入库单

    /** 出入库单：amount 为本位币价税合计；最后一行为合计（docNo 为「合计」） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TradeDocRow(String docNo, LocalDate bizDate, String partnerName, String orderNo, String warehouse,
                              BigDecimal totalQty, BigDecimal amount) {
    }

    abstract static class TradeDocsTool implements AiTool {

        private final ScmTradeDocService tradeDocService;
        private final ScmAiLookup lookup;
        private final WmsAiLookup wmsLookup;
        private final PermissionService permissionService;

        TradeDocsTool(ScmTradeDocService tradeDocService, ScmAiLookup lookup, WmsAiLookup wmsLookup, PermissionService permissionService) {
            this.tradeDocService = tradeDocService;
            this.lookup = lookup;
            this.wmsLookup = wmsLookup;
            this.permissionService = permissionService;
        }

        /** 第一个为默认（出库 / 入库），第二个为退货 */
        abstract TradeKind[] kinds();

        abstract String partnerKey();

        public String permission() {
            return "scm:" + kinds()[0].getPath() + ":list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("type", AiSchema.enumOf("scm.ai.param.tradeType", names(kinds())), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(PARTNER, AiSchema.string(partnerKey()).maxLength(64), false)
                    .prop(ORDER_NO, AiSchema.string("scm.ai.param.orderNo").maxLength(64), false)
                    .prop(WAREHOUSE, AiSchema.string("wms.ai.param.warehouse").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            String type = args.getString("type");
            TradeKind kind = type == null ? kinds()[0] : TradeKind.valueOf(type);
            // 退货单是另一个页面，需要它的查看权限
            if (!permissionService.hasPerm("scm:" + kind.getPath() + ":list")) {
                throw new BizException("scm.error.aiNoPermission", Messages.get(kind.getLabelKey()));
            }
            AiQuerySupport.Range range = AiQuerySupport.monthToDate(args);
            TradeDocQuery query = new TradeDocQuery(null, args.getString(ORDER_NO), null,
                    lookup.partnerId(kind.getOrderKind(), args.getString(PARTNER)), wmsLookup.warehouseId(args.getString(WAREHOUSE)),
                    range.begin(), range.end());
            List<ScmTradeDoc> docs = AiQuerySupport.fetch((c, s) -> tradeDocService.page(kind, c, s, query), MAX_DOCS);
            List<TradeDocRow> rows = new ArrayList<>(docs.stream().limit(limit(args)).map(d -> new TradeDocRow(d.getDocNo(),
                    d.getBizDate(), d.getPartnerName(), d.getOrderNo(), d.getWarehouseName(), plain(d.getTotalQty()),
                    d.getTotalAmount())).toList());
            rows.add(new TradeDocRow(Messages.get("scm.ai.text.total"), null, null, null, null,
                    plain(Decimals.sum(docs, ScmTradeDoc::getTotalQty)), Decimals.sum(docs, ScmTradeDoc::getTotalAmount)));
            return AiToolResult.rows(rows, (long) docs.size() + 1);
        }
    }

    @Component
    public static class SalesOutDocsTool extends TradeDocsTool {

        public SalesOutDocsTool(ScmTradeDocService tradeDocService, ScmAiLookup lookup, WmsAiLookup wmsLookup,
                                PermissionService permissionService) {
            super(tradeDocService, lookup, wmsLookup, permissionService);
        }

        TradeKind[] kinds() {
            return new TradeKind[]{TradeKind.SALES_OUT, TradeKind.SALES_RETURN};
        }

        String partnerKey() {
            return "scm.ai.param.customer";
        }

        public String name() {
            return "scm_sales_out_docs";
        }

        public String labelKey() {
            return "scm.ai.tool.salesOutDocs";
        }

        public String descriptionKey() {
            return "scm.ai.tool.salesOutDocs.desc";
        }
    }

    @Component
    public static class PurchaseInDocsTool extends TradeDocsTool {

        public PurchaseInDocsTool(ScmTradeDocService tradeDocService, ScmAiLookup lookup, WmsAiLookup wmsLookup,
                                  PermissionService permissionService) {
            super(tradeDocService, lookup, wmsLookup, permissionService);
        }

        TradeKind[] kinds() {
            return new TradeKind[]{TradeKind.PURCHASE_IN, TradeKind.PURCHASE_RETURN};
        }

        String partnerKey() {
            return "scm.ai.param.supplier";
        }

        public String name() {
            return "scm_purchase_in_docs";
        }

        public String labelKey() {
            return "scm.ai.tool.purchaseInDocs";
        }

        public String descriptionKey() {
            return "scm.ai.tool.purchaseInDocs.desc";
        }
    }
}
