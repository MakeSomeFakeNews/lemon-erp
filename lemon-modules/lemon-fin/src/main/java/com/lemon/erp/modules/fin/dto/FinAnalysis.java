package com.lemon.erp.modules.fin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析（毛利、存货对账、现金流量表、库龄、往来对账单）的返回结构。数据库查询结果用带 setter 的类
 * （MyBatis 按列名映射，H2 / MySQL 均适用），为空的列不输出（与原先返回 Map 时一致）；在 Java 中组装的结构用 record
 */
public final class FinAnalysis {

    private FinAnalysis() {
    }

    // ---------------------------------------------------------------- 销售毛利

    /**
     * 毛利汇总的一行：按物料、客户或月份汇总的销售出库 / 退货（不含税），成本 = 数量 × 当期加权平均单价；
     * missingCost 为没有当期核算单价（取参考成本价）的明细行数
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarginRow {
        /** 物料 ID、客户 ID 或月份（yyyyMM） */
        private Long groupKey;
        private String groupName;
        private BigDecimal qty;
        private BigDecimal revenue;
        private BigDecimal cost;
        private Long missingCost;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal profit;
        /** 毛利率（%），收入为 0 时为空 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal margin;
    }

    /** 毛利合计 */
    public record MarginTotal(BigDecimal revenue, BigDecimal cost, BigDecimal profit, BigDecimal margin) {
    }

    /** 销售毛利汇总 */
    public record GrossMargin(List<MarginRow> rows, MarginTotal total) {
    }

    /** 毛利明细：每条销售出库 / 退货明细，退货为负；costed 为 1 表示有当期核算单价 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarginDetailRow {
        private Long docId;
        private String docNo;
        private String kind;
        private LocalDate bizDate;
        private String customerName;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private BigDecimal qty;
        private BigDecimal revenue;
        private BigDecimal cost;
        private Integer costed;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal profit;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal margin;
    }

    /** 毛利明细，truncated 表示超出行数上限只返回了前面的部分 */
    public record MarginDetail(List<MarginDetailRow> rows, MarginTotal total, boolean truncated) {
    }

    // ---------------------------------------------------------------- 存货对账

    /** 成本核算期末金额按物料分类汇总 */
    @Data
    public static class CategoryAmount {
        private String category;
        private BigDecimal closeAmount;
    }

    /**
     * 存货科目：总账余额与成本核算期末金额（委托加工物资没有成本核算金额，为空）
     */
    public record InventoryAccount(String code, String name, BigDecimal glBalance, BigDecimal costAmount, BigDecimal diff) {
    }

    /** 成本核算期末数量与库存流水不一致的物料 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QtyDiff {
        private Long materialId;
        private String code;
        private String name;
        private BigDecimal costQty;
        private BigDecimal stockQty;
    }

    /** 存货对账 */
    public record InventoryReconciliation(int period, List<InventoryAccount> accounts, List<QtyDiff> qtyDiffs) {
    }

    // ---------------------------------------------------------------- 现金流量表

    /** 涉及现金、银行科目的已记账凭证的分录 */
    @Data
    public static class CashEntry {
        private Long voucherId;
        private String accountCode;
        private BigDecimal debit;
        private BigDecimal credit;
    }

    /** 现金流量表的行：标题行金额为空 */
    public record CashFlowLine(String name, BigDecimal amount, int level, boolean bold) {
    }

    /** 现金流量表，balanced 表示期初 + 净增加额与期末余额一致 */
    public record CashFlow(List<CashFlowLine> lines, BigDecimal closingBalance, boolean balanced) {
    }

    // ---------------------------------------------------------------- 库龄分析

    /** 截至日期的结存（数据库查询结果）及库龄分析结果 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StockAgeRow {
        private Long materialId;
        private String code;
        private String name;
        private String spec;
        private String unit;
        private String category;
        private BigDecimal qty;
        private BigDecimal unitCost;
        private BigDecimal amount;
        /** 结存对应的最早入库日期，入库记录不足以覆盖结存时为空 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private LocalDate oldestDate;
        private List<AgeCell> ages;
    }

    /** 入库记录（按物料、日期汇总） */
    @Data
    public static class StockInflow {
        private Long materialId;
        private LocalDate bizDate;
        private BigDecimal qty;
    }

    /** 物料截至期间最近一次核算的单价（没有时为参考成本价） */
    @Data
    public static class UnitCost {
        private Long materialId;
        private BigDecimal unitCost;
    }

    /** 库龄分段名称 */
    public record AgeBucket(String label) {
    }

    /** 一个库龄分段的数量和金额 */
    public record AgeCell(BigDecimal qty, BigDecimal amount) {
    }

    public record StockAgeTotal(BigDecimal qty, BigDecimal amount, List<AgeCell> ages) {
    }

    /** 库龄分析 */
    public record StockAge(LocalDate asOf, List<AgeBucket> buckets, List<StockAgeRow> rows, StockAgeTotal total) {
    }

    // ---------------------------------------------------------------- 往来对账单

    /** 往来流水（数据库查询结果）：应收应付单、收付款单、预收预付款 */
    @Data
    public static class PartnerFlow {
        private LocalDate bizDate;
        private PartnerLedgerType type;
        private String docNo;
        private String sourceType;
        private String sourceNo;
        private BigDecimal amount;
        private String remark;
    }

    /**
     * 对账单的一行：应收应付单计入增加，收付款计入减少（另一列为空）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StatementLine(LocalDate bizDate, PartnerLedgerType type, String docNo, String sourceType, String sourceNo,
                                String remark, @JsonInclude(JsonInclude.Include.ALWAYS) BigDecimal increase,
                                @JsonInclude(JsonInclude.Include.ALWAYS) BigDecimal decrease, BigDecimal balance) {
    }

    /** 往来对账单 */
    public record PartnerStatement(String partnerName, BigDecimal opening, List<StatementLine> rows, BigDecimal increase,
                                   BigDecimal decrease, BigDecimal closing) {
    }
}
