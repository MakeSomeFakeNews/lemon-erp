package com.lemon.erp.modules.mes.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.mes.dto.BoardView;
import com.lemon.erp.modules.mes.dto.MaterialKitView;
import com.lemon.erp.modules.mes.dto.WoKitView;
import com.lemon.erp.modules.mes.dto.WorkOrderQuery;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.model.BoardLineState;
import com.lemon.erp.modules.mes.model.BoardOrderState;
import com.lemon.erp.modules.mes.model.DeliveryRisk;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.KitStatus;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.mes.service.MesBoardService;
import com.lemon.erp.modules.mes.service.MesEquipmentService;
import com.lemon.erp.modules.mes.service.MesKitService;
import com.lemon.erp.modules.mes.service.MesOeeService;
import com.lemon.erp.modules.mes.service.MesWorkOrderService;
import com.lemon.erp.modules.wms.ai.AiQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.lemon.erp.modules.wms.ai.AiQuerySupport.END_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.LIMIT;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.START_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.limit;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.plain;

/**
 * 生产模块的 AI 问数工具：生产看板（工单进度、交期风险、产线负荷）、生产工单（含逾期）、齐套缺料、设备状态、设备 OEE。
 * 工单、齐套通过本模块 Service 查询（按工单的数据权限过滤）；看板、设备、OEE 与对应页面一样是全公司的数据
 */
public final class MesProductionAiTools {

    private static final String VIEW = "view";
    private static final String KEYWORD = "keyword";

    private MesProductionAiTools() {
    }

    /** 看板的查看内容：汇总指标 / 工单 / 产线 */
    enum BoardMode { KPI, ORDERS, LINES }

    /** 齐套的查看方式：按工单 / 按物料 */
    enum KitMode { ORDERS, MATERIALS }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }

    /** 0~1 的比率转为百分数（1 位小数） */
    static BigDecimal percent(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(Decimals.HUNDRED).setScale(1, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 生产看板

    /** 看板中的工单：progress 为完工进度（%），eta 为按产能推演的预计完工日期，risk 为交期风险 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BoardOrderRow(String woNo, String product, String materialCode, WorkOrderStatus status, BoardOrderState state,
                                DeliveryRisk risk, BigDecimal qty, BigDecimal finishedQty, BigDecimal progress, LocalDate dueDate,
                                LocalDate planEnd, LocalDate eta, String currentOp, String currentWorkCenter, BigDecimal todayGood,
                                String customerName, String salesOrderNo) {
    }

    /** 看板中的产线：utilization 为今日产能利用率（%），clearDate 为排队工单预计全部完成的日期 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BoardLineRow(String code, String name, BoardLineState state, BigDecimal capacityHours, BigDecimal todayHours,
                               BigDecimal todayGood, BigDecimal todayScrap, BigDecimal utilization, int queueOrders,
                               BigDecimal remainingHours, LocalDate clearDate) {
    }

    @Component
    @RequiredArgsConstructor
    public static class ProductionBoardTool implements AiTool {

        private final MesBoardService boardService;

        public String name() {
            return "mes_production_board";
        }

        public String labelKey() {
            return "mes.ai.tool.productionBoard";
        }

        public String descriptionKey() {
            return "mes.ai.tool.productionBoard.desc";
        }

        public String permission() {
            return "mes:board:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(VIEW, AiSchema.enumOf("mes.ai.param.boardView", names(BoardMode.values())), false)
                    .prop("risk", AiSchema.enumOf("mes.ai.param.deliveryRisk", names(DeliveryRisk.values())), false)
                    .prop(KEYWORD, AiSchema.string("mes.ai.param.woKeyword").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            BoardView board = boardService.board(null);
            String view = args.getString(VIEW);
            BoardMode mode = view == null ? BoardMode.ORDERS : BoardMode.valueOf(view);
            if (mode == BoardMode.KPI) {
                return AiToolResult.of(board.kpi());
            }
            if (mode == BoardMode.LINES) {
                return AiToolResult.rows(board.lines().stream().map(l -> new BoardLineRow(l.code(), l.name(), l.state(),
                        l.capacity(), l.todayHours(), plain(l.todayGood()), plain(l.todayScrap()), l.utilization(), l.queueOrders(),
                        l.remainingHours(), l.clearDate())).toList());
            }
            String risk = args.getString("risk");
            String kw = args.getString(KEYWORD) == null ? null : args.getString(KEYWORD).toLowerCase(Locale.ROOT);
            List<BoardOrderRow> rows = board.orders().stream()
                    .filter(o -> risk == null || o.risk() == DeliveryRisk.valueOf(risk))
                    .filter(o -> kw == null || contains(o.woNo(), kw) || contains(o.product(), kw) || contains(o.materialCode(), kw)
                            || contains(o.customerName(), kw) || contains(o.salesOrderNo(), kw))
                    .map(o -> new BoardOrderRow(o.woNo(), o.product(), o.materialCode(), o.status(), o.state(), o.risk(),
                            plain(o.qty()), plain(o.finishedQty()), o.progress(), o.dueDate(), o.planEnd(), o.eta(), o.currentOp(),
                            o.currentWorkCenter(), plain(o.todayGood()), o.customerName(), o.salesOrderNo()))
                    .toList();
            return AiToolResult.rows(rows.subList(0, Math.min(rows.size(), limit(args))), (long) rows.size());
        }
    }

    private static boolean contains(String s, String lowerKeyword) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    // ---------------------------------------------------------------- 生产工单

    /** 工单：progress 为完工进度（%），overdueDays 为计划完工已逾期的天数（未逾期或已完工时为空） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkOrderRow(String woNo, String productCode, String productName, String unit, BigDecimal qty,
                               BigDecimal finishedQty, BigDecimal progress, WorkOrderStatus status, LocalDate planStart,
                               LocalDate planEnd, Long overdueDays) {
    }

    @Component
    @RequiredArgsConstructor
    public static class WorkOrdersTool implements AiTool {

        private final MesWorkOrderService workOrderService;

        public String name() {
            return "mes_work_orders";
        }

        public String labelKey() {
            return "mes.ai.tool.workOrders";
        }

        public String descriptionKey() {
            return "mes.ai.tool.workOrders.desc";
        }

        public String permission() {
            return "mes:wo:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("woNo", AiSchema.string("mes.ai.param.woNo").maxLength(64), false)
                    .prop("product", AiSchema.string("mes.ai.param.product").maxLength(64), false)
                    .prop("status", AiSchema.enumOf("mes.ai.param.woStatus", names(WorkOrderStatus.values())), false)
                    .prop("overdueOnly", AiSchema.bool("mes.ai.param.overdueOnly"), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            LocalDate today = LocalDate.now();
            String status = args.getString("status");
            WorkOrderQuery query = new WorkOrderQuery(args.getString("woNo"), args.getString("product"),
                    status == null ? null : WorkOrderStatus.valueOf(status), args.getDate(START_DATE), args.getDate(END_DATE),
                    Boolean.TRUE.equals(args.getBoolean("overdueOnly")) ? today : null);
            PageResult<MesWorkOrder> page = workOrderService.page(1, limit(args), query);
            return AiToolResult.rows(page.records().stream().map(w -> new WorkOrderRow(w.getWoNo(), w.getMaterialCode(),
                    w.getMaterialName(), w.getUnit(), plain(w.getQty()), plain(w.getFinishedQty()),
                    Decimals.percent(w.getFinishedQty(), w.getQty(), 1), w.getStatus(), w.getPlanStart(), w.getPlanEnd(),
                    w.getStatus().isExecutable() && w.getPlanEnd() != null && w.getPlanEnd().isBefore(today)
                            ? ChronoUnit.DAYS.between(w.getPlanEnd(), today) : null)).toList(), page.total());
        }
    }

    // ---------------------------------------------------------------- 齐套缺料

    /** 工单齐套：kitRate 为齐套率（%），buildableQty 为按现有库存可生产的数量，shortLineCount 为缺料的用料行数 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KitOrderRow(String woNo, WorkOrderStatus status, String productCode, String productName, String unit,
                              BigDecimal qty, BigDecimal finishedQty, LocalDate planStart, int lineCount, int shortLineCount,
                              BigDecimal kitRate, BigDecimal buildableQty, KitStatus kitStatus) {
    }

    /** 物料缺料：requiredQty 全部未结束工单的需求，toIssueQty 待发料，onHandQty 可用库存，shortageQty 缺料，onOrderQty 在途采购 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KitMaterialRow(String materialCode, String materialName, String spec, String unit, SupplyType supplyType,
                                 int woCount, BigDecimal requiredQty, BigDecimal toIssueQty, BigDecimal onHandQty,
                                 BigDecimal shortageQty, BigDecimal onOrderQty, LocalDate requiredDate, String supplierName) {
    }

    @Component
    @RequiredArgsConstructor
    public static class KittingTool implements AiTool {

        private final MesKitService kitService;

        public String name() {
            return "mes_kitting";
        }

        public String labelKey() {
            return "mes.ai.tool.kitting";
        }

        public String descriptionKey() {
            return "mes.ai.tool.kitting.desc";
        }

        public String permission() {
            return "mes:kit:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(VIEW, AiSchema.enumOf("mes.ai.param.kitView", names(KitMode.values())), false)
                    .prop("woNo", AiSchema.string("mes.ai.param.woNo").maxLength(64), false)
                    .prop("kitStatus", AiSchema.enumOf("mes.ai.param.kitStatus", names(KitStatus.values())), false)
                    .prop("material", AiSchema.string("mes.ai.param.material").maxLength(64), false)
                    .prop("shortOnly", AiSchema.bool("mes.ai.param.shortOnly"), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            int limit = limit(args);
            if (args.has(VIEW) && KitMode.valueOf(args.getString(VIEW)) == KitMode.MATERIALS) {
                List<MaterialKitView> list = kitService.materials(args.getString("material"),
                        !Boolean.FALSE.equals(args.getBoolean("shortOnly")));
                return AiToolResult.rows(list.stream().limit(limit).map(m -> new KitMaterialRow(m.getMaterialCode(), m.getMaterialName(),
                        m.getSpec(), m.getUnit(), m.getSupplyType(), m.getWoCount(), plain(m.getRequiredQty()), plain(m.getToIssueQty()),
                        plain(m.getOnHandQty()), plain(m.getShortageQty()), plain(m.getOnOrderQty()), m.getRequiredDate(),
                        m.getSupplierName())).toList(), (long) list.size());
            }
            String kitStatus = args.getString("kitStatus");
            List<WoKitView> list = kitService.orders(args.getString("woNo"), kitStatus == null ? null : KitStatus.valueOf(kitStatus));
            return AiToolResult.rows(list.stream().limit(limit).map(w -> new KitOrderRow(w.getWoNo(), w.getStatus(), w.getProductCode(),
                    w.getProductName(), w.getUnit(), plain(w.getQty()), plain(w.getFinishedQty()), w.getPlanStart(), w.getLineCount(),
                    w.getShortLineCount(), w.getKitRate(), plain(w.getBuildableQty()), w.getKitStatus())).toList(), (long) list.size());
        }
    }

    // ---------------------------------------------------------------- 设备

    /** 设备台账：nextMaintainDate 为下次保养日期 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EquipmentRow(String code, String name, String model, String workCenter, EquipmentStatus status,
                               LocalDate lastMaintainDate, LocalDate nextMaintainDate) {
    }

    @Component
    @RequiredArgsConstructor
    public static class EquipmentTool implements AiTool {

        private final MesEquipmentService equipmentService;

        public String name() {
            return "mes_equipment";
        }

        public String labelKey() {
            return "mes.ai.tool.equipment";
        }

        public String descriptionKey() {
            return "mes.ai.tool.equipment.desc";
        }

        public String permission() {
            return "mes:equipment:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(KEYWORD, AiSchema.string("mes.ai.param.equipmentKeyword").maxLength(64), false)
                    .prop("status", AiSchema.enumOf("mes.ai.param.equipmentStatus", names(EquipmentStatus.values())), false)
                    .prop("maintainDueDays", AiSchema.integer("mes.ai.param.maintainDueDays").min(0).max(365), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            String status = args.getString("status");
            Long due = args.getLong("maintainDueDays");
            PageResult<MesEquipment> page = equipmentService.page(1, limit(args), args.getString(KEYWORD),
                    status == null ? null : EquipmentStatus.valueOf(status), null, due == null ? null : due.intValue());
            return AiToolResult.rows(page.records().stream().map(e -> new EquipmentRow(e.getCode(), e.getName(), e.getModel(),
                    e.getWorkCenterName(), e.getStatus(), e.getLastMaintainDate(), e.getNextMaintainDate())).toList(), page.total());
        }
    }

    /** 设备 OEE（期间合计）：availability 时间开动率、performance 性能开动率、quality 合格品率、oee 均为百分数 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OeeRow(String equipmentCode, String equipmentName, String workCenter, BigDecimal plannedHours,
                         BigDecimal runHours, BigDecimal downtimeHours, BigDecimal goodQty, BigDecimal scrapQty,
                         BigDecimal availability, BigDecimal performance, BigDecimal quality, BigDecimal oee) {
    }

    @Component
    @RequiredArgsConstructor
    public static class OeeTool implements AiTool {

        private final MesOeeService oeeService;

        public String name() {
            return "mes_equipment_oee";
        }

        public String labelKey() {
            return "mes.ai.tool.oee";
        }

        public String descriptionKey() {
            return "mes.ai.tool.oee.desc";
        }

        public String permission() {
            return "mes:oee:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop("equipment", AiSchema.string("mes.ai.param.equipmentKeyword").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            AiQuerySupport.Range range = AiQuerySupport.range(args, end -> end.minusDays(29));
            String kw = args.getString("equipment") == null ? null : args.getString("equipment").toLowerCase(Locale.ROOT);
            List<OeeRow> rows = oeeService.summary(range.begin(), range.end(), null).stream()
                    .filter(o -> kw == null || contains(o.equipmentCode(), kw) || contains(o.equipmentName(), kw))
                    .map(o -> new OeeRow(o.equipmentCode(), o.equipmentName(), o.workCenterName(), o.plannedHours(), o.runHours(),
                            o.downtimeHours(), plain(o.goodQty()), plain(o.scrapQty()), percent(o.availability()),
                            percent(o.performance()), percent(o.quality()), percent(o.oee())))
                    .toList();
            return AiToolResult.rows(rows.subList(0, Math.min(rows.size(), limit(args))), (long) rows.size());
        }
    }
}
