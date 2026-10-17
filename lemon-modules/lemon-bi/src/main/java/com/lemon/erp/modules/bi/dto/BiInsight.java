package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.bi.model.AbcClass;
import com.lemon.erp.modules.bi.model.AlertLevel;
import com.lemon.erp.modules.bi.model.AlertOperator;
import com.lemon.erp.modules.bi.model.BiCube;
import com.lemon.erp.modules.bi.model.DateFilter;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 经营驾驶舱、决策分析、预警、数据仓库状态的返回结构。数据库查询结果用带 setter 的类（MyBatis 按列名映射），
 * 为空的列不输出（与原先返回 Map 时一致）；在 Java 中组装的结构用 record
 */
public final class BiInsight {

    private BiInsight() {
    }

    // ---------------------------------------------------------------- 驾驶舱

    /** 数据仓库按月汇总：销售（金额、成本、毛利、数量）、采购（金额）、财务（收入、费用） */
    @Data
    public static class PeriodRow {
        private Integer period;
        private BigDecimal amount;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal qty;
        private BigDecimal revenue;
        private BigDecimal expense;
    }

    /** 驾驶舱月度趋势 */
    public record CockpitTrend(int period, BigDecimal sales, BigDecimal profit, BigDecimal purchase, BigDecimal revenue,
                               BigDecimal expense, BigDecimal netProfit) {
    }

    /**
     * 驾驶舱指标：销售同比环比等计算指标（为空也输出），以及实时汇总业务表的指标（数据库查询，为空不输出）
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CockpitKpi {
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthSales;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthSalesMom;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthSalesYoy;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthProfit;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthMargin;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal yearSales;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal yearSalesYoy;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal yearMargin;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal monthPurchase;
        private BigDecimal arBalance;
        private BigDecimal arOverdue;
        private BigDecimal apBalance;
        private BigDecimal openSalesAmount;
        private Long openWorkOrders;
        private Long lateWorkOrders;
        private Long activeProjects;
        private BigDecimal projectProgress;
        private Long headcount;
        private BigDecimal pipeline;
        private Long openTickets;
        private Long overdueTickets;
    }

    /** 排行：客户（金额、毛利）、物料（数量、金额） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopItem {
        private Long id;
        private String name;
        private BigDecimal qty;
        private BigDecimal amount;
        private BigDecimal profit;
    }

    /** 最新库存快照按物料分类 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InventoryCategory {
        private String category;
        private BigDecimal qty;
        private BigDecimal amount;
        private String snapDate;
    }

    /** 经营驾驶舱 */
    public record Cockpit(CockpitKpi kpi, List<CockpitTrend> trend, List<TopItem> topCustomers, List<TopItem> topMaterials,
                          List<InventoryCategory> inventory, LocalDateTime lastSync) {
    }

    // ---------------------------------------------------------------- 决策

    /** 月份与金额 */
    public record PeriodAmount(int period, BigDecimal amount) {
    }

    /** 销售预测 */
    public record Forecast(List<PeriodAmount> history, List<PeriodAmount> forecast, BigDecimal slope, boolean seasonal,
                           String method) {
    }

    /** ABC 分类的一行：数据源（近一年销售）+ 分类结果 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AbcRow {
        private Long materialId;
        private String code;
        private String name;
        private String category;
        private BigDecimal qty;
        private BigDecimal cost;
        private BigDecimal amount;
        /** 分类依据的值（销售成本或销售额） */
        private BigDecimal value;
        private BigDecimal share;
        private BigDecimal cumulative;
        @JsonProperty("class")
        private AbcClass abcClass;
    }

    /** ABC 各类汇总 */
    public record AbcSummary(@JsonProperty("class") AbcClass abcClass, long count, BigDecimal value, BigDecimal share) {
    }

    /** ABC 分类 */
    public record Abc(List<AbcRow> rows, List<AbcSummary> summary, String basis, BigDecimal total) {
    }

    /** 呆滞物料 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlowMovingRow {
        private Long materialId;
        private String code;
        private String name;
        private String category;
        private String unit;
        private BigDecimal qty;
        /** 最近出库时间（yyyy-MM-dd HH:mm:ss） */
        private String lastOut;
        /** 最早入库时间 */
        private String firstIn;
        private BigDecimal amount;
        /** 闲置天数，没有出入库记录时为空 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Long idleDays;
    }

    // ---------------------------------------------------------------- 预警

    /** 预警指标选项 */
    public record MetricOption(String key, String label, String unit) {
    }

    /** 预警规则的当前值与是否触发 */
    public record AlertResult(Long ruleId, String name, String metric, String metricLabel, String unit, AlertOperator operator,
                              BigDecimal threshold, AlertLevel level, BigDecimal value, boolean triggered, String remark) {
    }

    // ---------------------------------------------------------------- 数据仓库与多维分析

    /** 数据仓库各事实表的行数、是否正在同步、最近成功同步时间 */
    @Data
    public static class EtlOverview {
        private Long sales;
        private Long purchase;
        private Long inventory;
        private Long snapshots;
        private Long production;
        private Long finance;
        private Long hr;
        private Long crm;
        private Long project;
        private Boolean running;
        private LocalDateTime lastSuccess;
    }

    /** 多维分析的数据集（名称已翻译） */
    public record CubeInfo(String key, String label, DateFilter dateFilter, List<BiCube.BiDim> dims,
                           List<BiCube.BiMeasure> measures) {
    }

    // ---------------------------------------------------------------- 投屏

    /** 大屏选项 */
    public record ScreenOption(String key, String label) {
    }

    /** 投屏链接的公开信息 */
    public record ShareInfo(String name, String company, List<ScreenOption> screens, Integer intervalSeconds,
                            Long workCenterId, String workCenterName) {
    }
}
