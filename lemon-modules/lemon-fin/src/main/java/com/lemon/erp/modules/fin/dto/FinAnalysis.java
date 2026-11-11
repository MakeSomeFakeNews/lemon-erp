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
     * 毛利汇总的一行：按物料、客户或月份汇总的销售出库 / 退货（不含税，退货为负），口径同 issue #93：
     * 货款（优惠前）− 整单优惠 = 销售收入（revenue，记账的主营业务收入）；
     * 销售金额 = 销售收入 − 本公司承担运费 − 收款折让 / 抹零 − 坏账核销；毛利 = 销售金额 − 成本，毛利率 = 毛利 ÷ 销售金额。
     * 成本 = 数量 × 当期加权平均单价，missingCost 为没有当期核算单价（取参考成本价）的明细行数。
     * 整单优惠按行分摊（含税的分摊额按行税率折算为不含税）；坏账为销售出库单应收单上的核销金额、收款折让为这些单据应收单在收款时的折让
     * （多次收款累计），都按应收单的不含税比例折算、跟随出库单的业务日期归属；运费为本公司承担的发运运费等；三者按行金额分摊。
     * 折让、坏账、运费在财务报表中分别计入财务费用、营业外支出、销售费用，利润表的毛利 = 销售收入 − 成本，不扣它们
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarginRow {
        /** 物料 ID、客户 ID 或月份（yyyyMM） */
        private Long groupKey;
        private String groupName;
        private BigDecimal qty;
        /** 货款：优惠前的不含税金额 = 销售收入 + 整单优惠 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal goodsAmount;
        /** 整单优惠（不含税） */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal discount;
        /** 销售收入（扣整单优惠后，不含税） */
        private BigDecimal revenue;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal freight;
        /** 收款折让 / 抹零（不含税） */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal settleDiscount;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal badDebt;
        /** 销售金额 = 销售收入 − 运费 − 折让 − 坏账 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal salesAmount;
        private BigDecimal cost;
        private Long missingCost;
        /** 毛利 = 销售金额 − 成本 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal profit;
        /** 毛利率（%）= 毛利 ÷ 销售金额，销售金额为 0 时为空 */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal margin;
    }

    /** 毛利合计，字段含义见 {@link MarginRow} */
    public record MarginTotal(BigDecimal goodsAmount, BigDecimal discount, BigDecimal revenue, BigDecimal freight, BigDecimal settleDiscount,
                              BigDecimal badDebt, BigDecimal salesAmount, BigDecimal cost, BigDecimal profit, BigDecimal margin) {
    }

    /** 销售毛利汇总 */
    public record GrossMargin(List<MarginRow> rows, MarginTotal total) {
    }

    /**
     * 毛利明细：每条销售出库 / 退货明细，退货为负；costed 为 1 表示有当期核算单价。
     * 调整项（整单优惠、运费、折让、坏账）的显示方式（{@link com.lemon.erp.modules.fin.model.AdjustMode}）：
     * 汇总到底部时行上为优惠前的货款（销售金额 = 货款，毛利 = 货款 − 成本），调整项不分摊（为 0），在 {@link MarginDetail#adjustments} 中；
     * 分摊到各行时按行金额比例分摊，销售金额 = 销售收入 − 运费 − 折让 − 坏账
     */
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
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal goodsAmount;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal discount;
        /** 销售收入（扣整单优惠后） */
        private BigDecimal revenue;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal freight;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal settleDiscount;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal badDebt;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal salesAmount;
        private BigDecimal cost;
        private Integer costed;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal profit;
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private BigDecimal margin;
    }

    /**
     * 毛利明细，truncated 表示超出行数上限只返回了前面的部分；total 为全部行的合计（单据口径的销售金额与毛利，两种显示方式相同）；
     * adjustments 为汇总到底部时的调整行（只列出有金额的类别），各行销售金额 + 调整行金额 = 合计销售金额
     */
    public record MarginDetail(List<MarginDetailRow> rows, MarginTotal total, List<AdjustRow> adjustments, boolean truncated) {
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

    /** 核销了固定资产购置应付单的付款凭证 */
    @Data
    public static class AssetPayment {
        private Long voucherId;
        private BigDecimal assetAmount;
        private BigDecimal totalAmount;
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
