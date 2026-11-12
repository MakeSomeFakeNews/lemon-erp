package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lemon.erp.modules.fin.dto.AdjustRow;
import com.lemon.erp.modules.scm.dto.RfmSettings;
import com.lemon.erp.modules.scm.model.RfmSegment;
import com.lemon.erp.modules.scm.model.TradeKind;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 进销存报表的返回结构。金额均为本位币；销售额、进货额为不含税金额，另给出含税的价税合计；
 * 净额 = 出库（入库）− 退货，口径见 {@link SummaryRow}
 */
public final class JxcViews {

    private JxcViews() {
    }

    /**
     * 汇总行（销售：出库 / 退货；进货：入库 / 退货），合计行的 groupKey 为空。金额口径（issue #93）：
     * 货款（优惠前，不含税）− 整单优惠 = 净额（销售收入 / 进货金额，扣整单优惠后）；
     * 销售：销售金额 = 净额 − 本公司承担运费 − 收款折让 / 抹零 − 坏账核销，毛利 = 销售金额 − 成本，毛利率 = 毛利 ÷ 销售金额；
     * 进货：扣折让后净进货额 = 净额 − 付款折让 / 抹零（参考口径：付款折让计入财务费用，不冲减存货成本）。
     * 整单优惠按行分摊（含税的分摊额按行税率折算为不含税），运费、折让、坏账按单据归属、按行金额比例分摊到明细，
     * 所以按分类、品牌、商品汇总时只含所属明细的部分
     */
    @Data
    public static class SummaryRow {
        /** 维度取值：分类 / 品牌 / 区域的编码，客户 / 供应商 / 业务员 / 物料的 ID，日期，月份（202611）；为空表示未设置 */
        private String groupKey;
        private String groupName;
        /** 出库 / 入库数量 */
        private BigDecimal bizQty;
        private BigDecimal returnQty;
        private BigDecimal netQty;
        /** 出库 / 入库货款（优惠前，不含税） */
        private BigDecimal bizAmount;
        /** 退货货款（优惠前，不含税） */
        private BigDecimal returnAmount;
        /** 货款 = 出库（入库）货款 − 退货货款 */
        private BigDecimal goodsAmount;
        /** 整单优惠（不含税，退货为负） */
        private BigDecimal discount;
        /** 整单优惠（含税） */
        private BigDecimal discountTaxed;
        /** 净额 = 货款 − 整单优惠（不含税，即销售收入 / 进货金额） */
        private BigDecimal netAmount;
        /** 净价税合计（含税，扣整单优惠后） */
        private BigDecimal taxedAmount;
        /** 本公司承担的运费（只有销售报表） */
        private BigDecimal freight;
        /**
         * 收付款折让 / 抹零（issue #89）：销售报表为收款折让，进货报表为付款折让；这些单据的应收 / 应付单在收付款时的折让累计
         * （多次收付款累计），按单据的不含税比例折算、按行金额分摊，退货为负数
         */
        private BigDecimal settleDiscount;
        /** 坏账核销（销售出库单应收单上的核销金额，按该单的不含税比例折算，按行金额分摊；只有销售报表） */
        private BigDecimal badDebt;
        /** 销售金额 = 净额 − 运费 − 收款折让 − 坏账核销（只有销售报表） */
        private BigDecimal salesAmount;
        /** 扣折让后净进货额 = 净额 − 付款折让（只有进货报表） */
        private BigDecimal netAfterDiscount;
        /** 销售成本（只有销售报表） */
        private BigDecimal cost;
        /** 毛利 = 销售金额 − 成本 */
        private BigDecimal profit;
        /** 毛利率（%）= 毛利 ÷ 销售金额，销售金额为 0 时为空 */
        private BigDecimal margin;
        /** 平均单价（不含税）= 净额 / 净数量 */
        private BigDecimal avgPrice;
        /** 单据数（出库 / 入库 + 退货） */
        private Long docCount;
        /** 按参考成本价计算的明细行数（所在期间没有成本核算） */
        private Long missingCost;
    }

    /**
     * @param truncated 行数超过上限，只返回了净额最大的部分（合计仍为全部）
     */
    public record Summary(List<SummaryRow> rows, SummaryRow total, boolean truncated) {
    }

    /**
     * 明细行：退货的数量、金额为负数。调整项的显示方式（{@link JxcQuery#getAdjustMode()}）：
     * 汇总到底部时行上为优惠前的单价、价税合计、货款，销售金额 = 货款（进货的扣折让后金额 = 货款），毛利 = 货款 − 成本，
     * 调整项为 0，在 {@link DetailPage#adjustments()} 中按类别列出；分摊到各行时整单优惠、运费、折让、坏账按行分摊，
     * 销售金额 = 净额 − 运费 − 折让 − 坏账
     */
    @Data
    public static class DetailRow {
        private Long docId;
        private String docNo;
        private TradeKind kind;
        private LocalDate bizDate;
        private String partnerName;
        private String salesmanName;
        private String warehouseName;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private String category;
        private String brand;
        private BigDecimal qty;
        /** 含税单价（汇总到底部时为优惠前的单价） */
        private BigDecimal price;
        /** 优惠前含税单价（数据库取数，为空同单价） */
        @JsonIgnore
        private BigDecimal grossPrice;
        /** 货款（优惠前，不含税） */
        private BigDecimal goodsAmount;
        /** 本行分摊的整单优惠（不含税） */
        private BigDecimal discount;
        /** 本行分摊的整单优惠（含税，数据库取数） */
        @JsonIgnore
        private BigDecimal discountTaxed;
        /** 净额（扣整单优惠后，不含税） */
        private BigDecimal amount;
        /** 价税合计（汇总到底部时为优惠前） */
        private BigDecimal taxedAmount;
        private BigDecimal freight;
        /** 收付款折让 */
        private BigDecimal settleDiscount;
        private BigDecimal badDebt;
        /** 销售金额（只有销售报表） */
        private BigDecimal salesAmount;
        /** 扣折让后金额（只有进货报表） */
        private BigDecimal amountAfterDiscount;
        private BigDecimal cost;
        private BigDecimal profit;
        /** 1 已按当期核算成本，0 按参考成本价 */
        private Integer costed;
        private String remark;
    }

    /**
     * 明细（分页）
     *
     * @param adjustments 汇总到底部时的调整行（全部明细的合计，只列出有金额的类别；分摊到各行时为空）
     * @param summary     全部明细的合计（与汇总行一致；两种显示方式相同）
     */
    public record DetailPage(List<DetailRow> records, long total, List<AdjustRow> adjustments, SummaryRow summary) {
    }

    /** 导出用的明细（最多 {@code limit} 行），调整行与合计同 {@link DetailPage} */
    public record DetailExport(List<DetailRow> rows, boolean truncated, List<AdjustRow> adjustments, SummaryRow summary) {
    }

    /**
     * 单据行：汇总项下的一张出入库单 / 退货单，金额为该单中属于汇总项的明细行合计（与汇总同一口径，按分类、品牌、商品等汇总时
     * 只含匹配的明细，调整项也只含这些明细分摊的部分），退货的数量、金额为负数；金额口径同 {@link SummaryRow}
     */
    @Data
    public static class DocRow {
        private Long docId;
        private String docNo;
        private TradeKind kind;
        private LocalDate bizDate;
        private String partnerName;
        private String salesmanName;
        /** 表头仓库 */
        private String warehouseName;
        /** 属于汇总项的明细行数 */
        private Long lineCount;
        private BigDecimal qty;
        /** 货款（优惠前，不含税） */
        private BigDecimal goodsAmount;
        /** 整单优惠（不含税） */
        private BigDecimal discount;
        /** 净额（扣整单优惠后，不含税） */
        private BigDecimal amount;
        /** 价税合计 */
        private BigDecimal taxedAmount;
        private BigDecimal freight;
        /** 收付款折让（issue #89） */
        private BigDecimal settleDiscount;
        private BigDecimal badDebt;
        /** 销售金额（只有销售报表） */
        private BigDecimal salesAmount;
        /** 扣折让后金额（只有进货报表） */
        private BigDecimal amountAfterDiscount;
        private BigDecimal cost;
        private BigDecimal profit;
        /** 毛利率（%），销售金额为 0 时为空 */
        private BigDecimal margin;
        /** 按参考成本价计算的明细行数 */
        private Long missingCost;
        private String remark;
    }

    /**
     * 单据列表（分页）
     *
     * @param summary 全部单据的合计（与汇总行一致）
     */
    public record DocPage(List<DocRow> records, long total, SummaryRow summary) {
    }

    /** 导出用的单据列表（最多 {@code limit} 行），summary 为全部单据的合计 */
    public record DocExport(List<DocRow> rows, boolean truncated, SummaryRow summary) {
    }

    // ---------------------------------------------------------------- 经营状况

    /** 资金流：收款（收款单 + 预收款 + 票据托收、贴现）、付款（付款单 + 预付款 + 应付票据兑付）；票据业务生成的收付款单不是现金收付，不计入 */
    @Data
    public static class CashRow {
        private Integer period;
        private BigDecimal receipt;
        private BigDecimal payment;
        private BigDecimal net;
    }

    /**
     * @param total     期间的销售合计
     * @param trend     按月的销售与利润
     * @param cash      按月的资金流，没有「全部数据」权限时为空
     * @param salesmen  员工销售情况
     * @param customers 客户销售分布：净额前 10 的客户，其余合并为一行（groupKey 为空、groupName 为空）
     */
    public record Business(SummaryRow total, List<SummaryRow> trend, List<CashRow> cash, List<SummaryRow> salesmen,
                           List<SummaryRow> customers, int customerCount) {
    }

    // ---------------------------------------------------------------- 资产状况

    /** 库存金额（按仓库、物料分类） */
    @Data
    public static class InventoryRow {
        private Long warehouseId;
        private String warehouseName;
        private String category;
        private BigDecimal qty;
        private BigDecimal amount;
        /** 1 用到了升级前回填的近似成本流水 */
        private Integer estimated;
    }

    /** 名称 + 金额 */
    public record NameAmount(String key, String name, BigDecimal amount) {
    }

    /** 往来余额（数据库查询结果） */
    @Data
    public static class Balances {
        private BigDecimal arBalance;
        private BigDecimal arOverdue;
        private BigDecimal apBalance;
        private BigDecimal advanceReceived;
        private BigDecimal advancePaid;
        /** 持有的应收票据、未兑付的应付票据（票面金额） */
        private BigDecimal notesReceivable;
        private BigDecimal notesPayable;
    }

    /** 已生效、还没有生成凭证的收付款（资金余额取自凭证，不含它们） */
    @Data
    public static class Unvouchered {
        private Long count;
        private BigDecimal receipt;
        private BigDecimal payment;
    }

    /** 往来单位余额排行（数据库查询结果） */
    @Data
    public static class PartnerBalance {
        private Long partnerId;
        private String partnerName;
        private BigDecimal amount;
    }

    /** 资金账户余额（现金、银行的末级科目） */
    public record FundRow(Long accountId, String code, String name, BigDecimal balance) {
    }

    /**
     * 资产状况（截至某日，默认今天）。净资产 = 资金 + 库存 + 应收 + 应收票据 + 预付 − 应付 − 应付票据 − 预收
     *
     * @param live               截至今天（实时）
     * @param inventoryEstimated 库存金额用到了升级前回填的近似成本流水（截至日早于实时成本上线时）
     * @param unvouchered        截至日已生效、还没有生成凭证的收付款（不在资金余额中），没有时 count 为 0
     */
    public record Assets(LocalDate asOf, boolean live, BigDecimal cashBalance, BigDecimal bankBalance, List<FundRow> funds,
                         BigDecimal inventoryAmount, boolean inventoryEstimated, List<NameAmount> inventoryByWarehouse,
                         List<NameAmount> inventoryByCategory, Balances balances, List<PartnerBalance> topReceivables,
                         List<PartnerBalance> topPayables, Unvouchered unvouchered, BigDecimal netAssets) {
    }

    // ---------------------------------------------------------------- 商品分析

    /** 商品分析行：各标签页使用其中的部分字段 */
    @Data
    public static class GoodsRow {
        private Long materialId;
        private String code;
        private String name;
        private String spec;
        private String unit;
        private String category;
        private String brand;
        /** 期间净销量、净销售额（不含税）、成本、毛利 */
        private BigDecimal salesQty;
        private BigDecimal salesAmount;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin;
        /** 最近一次销售出库日期（截至结束日期） */
        private LocalDate lastSaleDate;
        /** 当前库存（不含供应商寄售仓）、库存金额 */
        private BigDecimal stockQty;
        private BigDecimal stockAmount;
        /** 可用库存（不含不良品仓、寄售仓和冻结数量）、安全库存、未出库的销售订单、未入库的采购订单 */
        private BigDecimal availableQty;
        private BigDecimal safetyStock;
        private BigDecimal soOpen;
        private BigDecimal poOpen;
        /** 缺口 = 安全库存 + 销售待发 − 可用库存 */
        private BigDecimal gapQty;
        /** 可销天数 = 库存 / 期间日均销量 */
        private BigDecimal coverDays;
        /** 期初、期末库存数量（按单据业务日期） */
        private BigDecimal openQty;
        private BigDecimal closeQty;
        /** 周转次数 = 净销量 / 平均库存（(期初 + 期末) / 2）；周转天数 = 期间天数 / 周转次数 */
        private BigDecimal turnover;
        private BigDecimal turnoverDays;
    }

    // ---------------------------------------------------------------- 积压分析（issue #111）

    /**
     * 积压分析的一行：积压数量 = 库存数量 − 积压基数（只列出大于 0 的）。合计行只有数量、资金占用
     */
    @Data
    public static class OverstockRow {
        private Long materialId;
        private String code;
        private String name;
        private String spec;
        private String unit;
        private String category;
        private String brand;
        /** 登记日期（物料资料的创建日期） */
        private LocalDate createDate;
        /** 最新进货（截至日之前最近一次采购入库）、最新出库（最近一次销售出库）的日期，全部仓库 */
        private LocalDate lastPurchaseDate;
        private LocalDate lastSaleDate;
        /** 截至日的库存数量（所选仓库，不含供应商寄售仓） */
        private BigDecimal stockQty;
        /** 在库天数 = 截至日 − 最新进货日期（没有进货时取登记日期） */
        private Long stockDays;
        private BigDecimal baseQty;
        /** 积压系数 = 库存数量 ÷ 积压基数，基数为 0 时为空 */
        private BigDecimal ratio;
        private BigDecimal overQty;
        /** 资金占用 = 积压数量 × 库存均价（实时移动加权平均，没有时取最近一期核算单价、参考成本价），需要「查看成本金额」权限 */
        private BigDecimal amount;
        /** 合计行：积压的物料数 */
        private Long materialCount;
    }

    /**
     * 积压分析的分页结果
     *
     * @param summary    全部积压物料的合计（materialCount、stockQty、baseQty、overQty、amount）
     * @param salesBegin 积压基数为销量时的统计期间，库存上限时为空
     */
    public record OverstockPage(List<OverstockRow> records, long total, OverstockRow summary, LocalDate date,
                                LocalDate salesBegin, LocalDate salesEnd, boolean costVisible) {
    }

    // ---------------------------------------------------------------- RFM

    @Data
    public static class RfmRow {
        private Long customerId;
        private String code;
        private String name;
        private LocalDate lastDate;
        /** 最近一次购买距统计截止日的天数 */
        private Long recency;
        /** 期间的销售出库单数 */
        private Long frequency;
        /** 期间净销售额（不含税） */
        private BigDecimal monetary;
        /** 每月订货次数 = 出库单数 ÷ 统计月数 */
        private BigDecimal monthlyFrequency;
        /** 平均每次订货金额 = 净销售额 ÷ 出库单数 */
        private BigDecimal avgAmount;
        /** 五分位得分 1–5 */
        private Integer r;
        private Integer f;
        private Integer m;
        /** R / F / M 是否为「高」（按所选的分群方式） */
        private boolean recent;
        private boolean frequent;
        private boolean valuable;
        private RfmSegment segment;
    }

    public record SegmentStat(RfmSegment segment, int count, BigDecimal monetary) {
    }

    /**
     * @param asOf     计算最近购买天数的截止日（结束日期与今天中较早的一个）
     * @param months   统计月数（开始日期到截止日），用于计算每月订货次数
     * @param settings 实际使用的分群参数
     */
    public record Rfm(LocalDate asOf, BigDecimal months, RfmSettings settings, List<RfmRow> rows, List<SegmentStat> segments,
                      BigDecimal avgR, BigDecimal avgF, BigDecimal avgM) {
    }
}
