package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.mes.dto.BoardHourRow;
import com.lemon.erp.modules.mes.dto.BoardView;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据大屏的返回结构：每个大屏一个 record（都带 key、title、time），统计查询的结果用带 setter 的类
 * （MyBatis 按列名映射，为空的列不输出，与原先返回 Map 时一致）
 */
public final class BiScreen {

    private BiScreen() {
    }

    /** 大屏数据 */
    public sealed interface ScreenData permits Factory, Progress, Warehouse, Sales, Quality {
        String key();

        String title();

        /** 生成时间（ISO 格式，不含毫秒） */
        String time();
    }

    // ---------------------------------------------------------------- 生产车间

    public record Factory(String key, String title, String time, String workCenter, FactoryKpi kpi, List<ReportDay> trend,
                          List<WorkCenterLoad> workCenters, List<Equipment> equipments, List<StopReason> stopReasons,
                          List<ActiveWorkOrder> workOrders, List<RecentReport> reports) implements ScreenData {
    }

    /** todayFinishedIn 只在不按工作中心统计时有值 */
    public record FactoryKpi(BigDecimal todayGood, BigDecimal todayScrap, BigDecimal todayYield, BigDecimal todayHours,
                             BigDecimal monthGood, BigDecimal monthYield, BigDecimal todayFinishedIn, Long activeOrders,
                             Long overdueOrders, Long completedOrders, int equipments, long runningEquipments,
                             BigDecimal runningRate) {
    }

    /** 报工合计 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReportSum {
        private BigDecimal good;
        private BigDecimal scrap;
        private BigDecimal hours;
    }

    /** 每日报工（没有报工的日期补 0） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReportDay {
        private LocalDate day;
        private BigDecimal good;
        private BigDecimal scrap;
    }

    /** 工作中心当日负荷 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WorkCenterLoad {
        private String name;
        private BigDecimal capacity;
        private BigDecimal hours;
        private BigDecimal good;
        private BigDecimal scrap;
    }

    /** 设备：当日合格数、本月停机工时 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Equipment {
        private String code;
        private String name;
        private EquipmentStatus status;
        private String statusLabel;
        private String workCenter;
        private BigDecimal good;
        private BigDecimal stopHours;
    }

    /** 停机原因 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StopReason {
        private String label;
        private BigDecimal hours;
        private String value;
    }

    /** 在制工单 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActiveWorkOrder {
        private String woNo;
        private String product;
        private BigDecimal qty;
        private BigDecimal finishedQty;
        private LocalDate planEnd;
        private String status;
        private BigDecimal doneQty;
        private String currentOp;
    }

    /** 工单数量：在制、逾期、当日完工 */
    @Data
    public static class WorkOrderCounts {
        private Long active;
        private Long overdue;
        private Long completed;
    }

    /** 最新报工 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentReport {
        private LocalDateTime time;
        private LocalDate day;
        private String woNo;
        private String op;
        private String operator;
        private String equipment;
        private BigDecimal good;
        private BigDecimal scrap;
    }

    // ---------------------------------------------------------------- 生产进度

    /** 与「生产看板」相同的数据 */
    public record Progress(String key, String title, String time, String workCenter, BoardView.Kpi kpi,
                           List<BoardView.Line> lines, List<BoardView.Order> orders, List<BoardHourRow> hourly) implements ScreenData {
    }

    // ---------------------------------------------------------------- 仓储物流

    public record Warehouse(String key, String title, String time, WarehouseKpi kpi, List<StockDay> trend,
                            List<StockDocType> byType, List<WarehouseStock> warehouses, List<LowStock> lowStock,
                            List<ExpiringBatch> expiring, List<PendingDelivery> deliveries, List<RecentStockDoc> docs) implements ScreenData {
    }

    public record WarehouseKpi(Long todayInDocs, BigDecimal todayInQty, Long todayOutDocs, BigDecimal todayOutQty,
                               Long pendingPick, Long pendingShip, Long shippedToday, int transit, Long skus,
                               BigDecimal stockQty, int lowStock, int expiring) {
    }

    /** 已完成的出入库单合计 */
    @Data
    public static class StockDocSum {
        private StockBizType kind;
        private Long docs;
        private BigDecimal qty;
    }

    /** 每日出入库数量（数据库按出入库分行） */
    @Data
    public static class StockDocDay {
        private LocalDate day;
        private StockBizType kind;
        private BigDecimal qty;
    }

    /** 每日出入库数量（入库、出库合并为一行，没有的日期补 0） */
    @Data
    public static class StockDay {
        private LocalDate day;
        private BigDecimal inQty;
        private BigDecimal outQty;
    }

    /** 按出入库类型 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StockDocType {
        private StockBizType kind;
        private String label;
        private BigDecimal qty;
        private String value;
    }

    /** 各仓库库存 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseStock {
        private String name;
        private BigDecimal qty;
        private Long skus;
    }

    /** 库存概况 */
    @Data
    public static class StockOverview {
        private Long skus;
        private BigDecimal qty;
    }

    /** 低于安全库存 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LowStock {
        private String code;
        private String name;
        private String unit;
        private BigDecimal qty;
        private BigDecimal safety;
    }

    /** 临期 / 过期批次 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExpiringBatch {
        private String code;
        private String name;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal qty;
    }

    /** 待发货的发货通知 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PendingDelivery {
        private String deliveryNo;
        private String customer;
        private String warehouse;
        private LocalDate shipDate;
        private String status;
        private String statusLabel;
    }

    /** 发货通知数量：待拣货、待发货、当日已发货 */
    @Data
    public static class DeliveryCounts {
        private Long pending;
        private Long picked;
        private Long shipped;
    }

    /** 最近完成的出入库单 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentStockDoc {
        private String docNo;
        private StockBizType kind;
        private String typeLabel;
        private String warehouse;
        private BigDecimal qty;
        private LocalDateTime time;
        private String bizType;
    }

    // ---------------------------------------------------------------- 销售经营

    public record Sales(String key, String title, String time, SalesKpi kpi, List<SalesMonth> monthly,
                        List<NameAmount> topCustomers, List<NameAmount> topProducts, List<NameAmount> salesmen,
                        List<OrderStatus> orderStatus, List<RecentOrder> orders) implements ScreenData {
    }

    public record SalesKpi(BigDecimal todayOrderAmount, Long todayOrders, BigDecimal monthOrderAmount, BigDecimal yearOrderAmount,
                           BigDecimal todayShipped, BigDecimal monthShipped, BigDecimal yearShipped, BigDecimal lastYearShipped,
                           BigDecimal monthReceipt, BigDecimal yearReceipt, BigDecimal receivable, int overdueOrders,
                           int newCustomers) {
    }

    /** 本年各月：订单金额、出库金额（未到的月份为空）、上年同月出库金额 */
    public record SalesMonth(int month, BigDecimal orders, BigDecimal shipped, BigDecimal lastYear) {
    }

    /** 已审核的销售订单合计 */
    @Data
    public static class OrderSum {
        private BigDecimal amount;
        private Long orders;
    }

    /** 按月金额 */
    @Data
    public static class PeriodAmount {
        private Integer period;
        private BigDecimal amount;
    }

    /** 排行：客户（金额）、产品（金额、数量）、业务员（金额、订单数） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NameAmount {
        private String name;
        private BigDecimal amount;
        private BigDecimal qty;
        private Long orders;
    }

    /** 执行中的销售订单按状态 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderStatus {
        private String status;
        private String label;
        private Long orders;
        private BigDecimal amount;
    }

    /** 最新销售订单 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentOrder {
        private String orderNo;
        private String customer;
        private BigDecimal amount;
        private String status;
        private String statusLabel;
        private LocalDate orderDate;
    }

    // ---------------------------------------------------------------- 质量管理

    public record Quality(String key, String title, String time, QualityKpi kpi, List<InspectionType> byType,
                          List<InspectionDay> trend, List<DefectType> defects, List<SupplierQuality> suppliers,
                          List<OpenNcr> ncrs, List<RecentInspection> inspections) implements ScreenData {
    }

    public record QualityKpi(long inspections, BigDecimal passRate, BigDecimal iqcRate, BigDecimal processRate,
                             BigDecimal productionYield, BigDecimal scrapQty, Long openNcr, Long monthNcr, Long doneNcr) {
    }

    /** 已确认的检验单按类型 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InspectionType {
        private QcType type;
        private String label;
        private Long total;
        private Long pass;
        private Long fail;
        private Long concession;
        /** 合格率（%） */
        private BigDecimal rate;
    }

    /** 每日检验（没有检验的日期补 0，合格率为空） */
    @Data
    public static class InspectionDay {
        private LocalDate day;
        private Long total;
        private Long pass;
        private BigDecimal rate;
    }

    /** 不良类型 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DefectType {
        private String label;
        private Long times;
        private BigDecimal qty;
        private String value;
    }

    /** 供应商来料质量 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SupplierQuality {
        private String name;
        private Long total;
        private Long fail;
        private BigDecimal rate;
    }

    /** 不合格品处理数量：未处置、期间新建、期间处置完成 */
    @Data
    public static class NcrCounts {
        private Long open;
        private Long created;
        private Long done;
    }

    /** 未处置的不合格品 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OpenNcr {
        private String ncrNo;
        private String material;
        private BigDecimal qty;
        private String sourceLabel;
        private String defectLabel;
        private String statusLabel;
        private LocalDateTime time;
        private String source;
        private String defectType;
        private String status;
    }

    /** 最近的检验 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentInspection {
        private String inspNo;
        private String typeLabel;
        private String material;
        private String result;
        private String resultLabel;
        private BigDecimal defectQty;
        private LocalDate date;
        private String type;
    }
}
