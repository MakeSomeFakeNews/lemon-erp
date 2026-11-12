package com.lemon.erp.modules.fin.model;

import java.util.Map;

/**
 * 自动凭证科目：业务单据生成凭证时各用途使用的科目。
 * 默认取 init.sql 预置科目的编码，可在「凭证科目设置」中改为其他末级科目；带维度的用途可以再按物料分类 / 职位类别 / 收发类别分别指定。
 * 科目范围用途（{@link #scope()}，如库存现金、银行存款）设置的是一个科目及其全部下级，可以是上级科目，用于识别结算科目、现金流量等
 */
public enum AccountRole {

    /** 库存现金：其下级末级科目可作为收付款的结算账户，现金流量表、资金计划按此识别现金 */
    CASH("fin.label.cashCashEquivalents", "fin.label.cashOnHand", "1001", true),
    /** 银行存款：其下级末级科目可作为结算账户、银行对账的账户 */
    BANK("fin.label.cashCashEquivalents", "fin.label.bankDeposits", "1002", true),

    RECEIVABLE("fin.label.receivablesPayables", "fin.label.accountsReceivable", "1122"),
    PAYABLE("fin.label.receivablesPayables", "fin.label.accountsPayable", "2202"),
    ADVANCE_RECEIVED("fin.label.receivablesPayables", "fin.label.advancesCustomers", "2203"),
    PREPAYMENT("fin.label.receivablesPayables", "fin.label.prepayments", "1123"),
    NOTES_RECEIVABLE("fin.label.receivablesPayables", "fin.label.notesReceivable", "1121"),
    NOTES_PAYABLE("fin.label.receivablesPayables", "fin.label.notesPayable", "2201"),
    OTHER_RECEIVABLE("fin.label.receivablesPayables", "fin.label.otherReceivablesEmployeeLoans", "1221"),
    OTHER_PAYABLE("fin.label.receivablesPayables", "fin.label.otherPayablesExpensesWithholdings", "2241"),
    /** 代垫运费（客户承担的发运运费）：付承运商时 借 该科目 / 贷 应付账款，向客户收取时 借 应收账款 / 贷 该科目，两边相抵 */
    FREIGHT_ADVANCE("fin.label.receivablesPayables", "fin.label.freightAdvance", "1221"),

    /** 默认：低值易耗品 → 周转材料，半成品、成品 → 库存商品，其余 → 原材料 */
    INVENTORY("fin.label.inventoryCost", "fin.label.inventories", "1403", Dimension.MATERIAL_CATEGORY,
            Map.of(FinConstants.LOW_VALUE_CATEGORY, "1411", FinConstants.CATEGORY_SEMI, "1405", FinConstants.CATEGORY_FINISHED, "1405")),
    OUTSOURCE_MATERIAL("fin.label.inventoryCost", "fin.label.materialsSubcontracting", "1408"),
    PRODUCTION_COST("fin.label.inventoryCost", "fin.label.productionCost", "5001"),
    MANUFACTURING_OVERHEAD("fin.label.inventoryCost", "fin.label.manufacturingOverhead", "5101"),
    CONTRACT_COST("fin.label.inventoryCost", "fin.label.contractPerformanceCostProjects", "5401"),
    PENDING_LOSS("fin.label.inventoryCost", "fin.label.pendingPropertyGainsLossesCountSurplus", "1901"),
    /** 其他入库（按入库类别）的对方科目：成本结转凭证 借 存货 / 贷 该科目；未设置时同待处理财产损溢 */
    OTHER_IN_OFFSET("fin.label.inventoryCost", "fin.label.otherInOffset", "1901", Dimension.IN_CATEGORY),
    /** 其他出库、报废出库（按出库类别）的对方科目：借 该科目 / 贷 存货，如福利领用 → 应付职工薪酬、研发领用 → 研发支出；未设置时同待处理财产损溢 */
    OTHER_OUT_OFFSET("fin.label.inventoryCost", "fin.label.otherOutOffset", "1901", Dimension.OUT_CATEGORY),

    REVENUE("fin.label.revenueExpenses", "fin.label.mainOperatingRevenue", "6001", Dimension.MATERIAL_CATEGORY),
    COST_OF_SALES("fin.label.revenueExpenses", "fin.label.mainOperatingCost", "6401", Dimension.MATERIAL_CATEGORY),
    OTHER_REVENUE("fin.label.revenueExpenses", "fin.label.otherOperatingRevenueServiceFees", "6051"),
    CONTRACT_LIABILITY("fin.label.revenueExpenses", "fin.label.contractLiabilitiesProjectInvoicing", "2205"),
    SELLING_EXPENSE("fin.label.revenueExpenses", "fin.label.sellingExpensesFreightAfterSalesParts", "6601"),
    /** 销售运费（本公司承担的发运运费）：借 该科目 / 贷 应付账款-承运商 */
    SALES_FREIGHT("fin.label.revenueExpenses", "fin.label.salesFreight", "6601"),
    /**
     * 采购运费（本公司承担、业务参数「本公司承担的采购运费计入」为当期费用时）：借 该科目 / 贷 应付账款-承运商。
     * 默认销售费用（商品流通企业的进货费用），计入存货成本时不用此科目
     */
    PURCHASE_FREIGHT("fin.label.revenueExpenses", "fin.label.purchaseFreightExpense", "6601"),
    /**
     * 调拨费用（调拨单的配送费，月结的承运商应付）：借 该科目 / 贷 应付账款-承运商。默认销售费用（门店配送属于销售环节的运输费）；
     * 同一公司内部调拨不增加存货价值，不计入存货成本
     */
    TRANSFER_FEE("fin.label.revenueExpenses", "fin.label.transferFee", "6601"),
    TRAINING_EXPENSE("fin.label.revenueExpenses", "fin.label.trainingExpenses", "660205"),
    FINANCE_EXPENSE("fin.label.revenueExpenses", "fin.label.financialExpensesFeesDiscountsFxGains", "6603"),
    NON_OPERATING_INCOME("fin.label.revenueExpenses", "fin.label.nonOperatingIncome", "6301"),
    NON_OPERATING_EXPENSE("fin.label.revenueExpenses", "fin.label.nonOperatingExpenses", "6711"),
    /** 收款折让 / 抹零：借 该科目（默认财务费用，可改为销售费用、主营业务收入的折让明细科目等） */
    RECEIPT_DISCOUNT("fin.label.revenueExpenses", "fin.label.receiptDiscount", "6603"),
    /** 付款折让 / 抹零：贷 该科目（默认财务费用） */
    PAYMENT_DISCOUNT("fin.label.revenueExpenses", "fin.label.paymentDiscount", "6603"),
    /** 应收冲账（垫付运费、销售返利、三包赔偿等抵扣客户欠款）：借 该科目 / 贷 应收账款，冲账单上可另选科目 */
    OFFSET_AR("fin.label.revenueExpenses", "fin.label.offsetArAccount", "6601"),
    /** 应付冲账（供应商返利、赔偿等抵扣欠供应商的款）：借 应付账款 / 贷 该科目（默认冲减主营业务成本），冲账单上可另选科目 */
    OFFSET_AP("fin.label.revenueExpenses", "fin.label.offsetApAccount", "6401"),
    /** 采购返利（供应商给我方的返利，结算生成红字应付单）：借 应付账款 / 贷 该科目（默认冲减主营业务成本）、进项税额 */
    PURCHASE_REBATE("fin.label.revenueExpenses", "fin.label.purchaseRebateAccount", "6401"),

    INPUT_VAT("fin.label.taxes", "fin.label.inputVat", "222101"),
    OUTPUT_VAT("fin.label.taxes", "fin.label.outputVat", "222102"),
    UNPAID_VAT("fin.label.taxes", "fin.label.unpaidVat", "222104"),
    CITY_TAX("fin.label.taxes", "fin.label.urbanConstructionTaxPayable", "222105"),
    EDU_SURCHARGE("fin.label.taxes", "fin.label.educationSurchargePayable", "222106"),
    LOCAL_EDU_SURCHARGE("fin.label.taxes", "fin.label.localEducationSurchargePayable", "222107"),
    TAX_SURCHARGE("fin.label.taxes", "fin.label.taxesSurcharges", "6403"),
    INCOME_TAX("fin.label.taxes", "fin.label.individualIncomeTaxPayable", "222103"),

    FIXED_ASSET("fin.label.assets", "fin.label.fixedAssets", "1601"),
    DEPRECIATION("fin.label.assets", "fin.label.accumulatedDepreciation", "1602"),
    ASSET_DISPOSAL("fin.label.assets", "fin.label.fixedAssetsPendingDisposal", "1606"),

    SALARY("fin.label.payroll", "fin.label.employeeBenefitsPayable", "2211"),
    /** 默认：生产岗位 → 制造费用（成本核算时按工时分配到工单），销售岗位 → 销售费用，其余 → 管理费用-工资 */
    SALARY_EXPENSE("fin.label.payroll", "fin.label.salaryExpenses", "660201", Dimension.POSITION_CATEGORY, Map.of(FinConstants.POSITION_PRODUCTION, "5101", FinConstants.POSITION_SALES, "6601")),

    PROFIT_THIS_YEAR("fin.label.periodEnd", "fin.label.currentYearProfit", "4103"),
    PROFIT_DISTRIBUTION("fin.label.periodEnd", "fin.label.profitDistribution", "4104");

    /** 用途可以按什么分别指定科目 */
    public enum Dimension {
        NONE(null),
        /** 字典 wms_material_category */
        MATERIAL_CATEGORY("wms_material_category"),
        /** 字典 hr_position_category */
        POSITION_CATEGORY("hr_position_category"),
        /** 字典 wms_in_category：入库类别 */
        IN_CATEGORY("wms_in_category"),
        /** 字典 wms_out_category：出库类别 */
        OUT_CATEGORY("wms_out_category");

        private final String dictType;

        Dimension(String dictType) {
            this.dictType = dictType;
        }

        public String dictType() {
            return dictType;
        }
    }

    /** 分组、名称的消息键 */
    private final String group;
    private final String label;
    private final String defaultCode;
    private final Dimension dimension;
    /** 维度取值 → 默认科目编码，没有的取 defaultCode */
    private final Map<String, String> dimensionDefaults;
    /** 科目范围：设置的科目及其全部下级都属于该用途，可以是上级科目，不用于生成凭证 */
    private final boolean scope;

    AccountRole(String group, String label, String defaultCode) {
        this(group, label, defaultCode, Dimension.NONE, Map.of());
    }

    AccountRole(String group, String label, String defaultCode, boolean scope) {
        this(group, label, defaultCode, Dimension.NONE, Map.of(), scope);
    }

    AccountRole(String group, String label, String defaultCode, Dimension dimension) {
        this(group, label, defaultCode, dimension, Map.of());
    }

    AccountRole(String group, String label, String defaultCode, Dimension dimension, Map<String, String> dimensionDefaults) {
        this(group, label, defaultCode, dimension, dimensionDefaults, false);
    }

    AccountRole(String group, String label, String defaultCode, Dimension dimension, Map<String, String> dimensionDefaults, boolean scope) {
        this.group = group;
        this.label = label;
        this.defaultCode = defaultCode;
        this.dimension = dimension;
        this.dimensionDefaults = dimensionDefaults;
        this.scope = scope;
    }

    /** 是否科目范围用途：设置的科目及其全部下级都属于该用途（按编码前缀匹配），可以是上级科目 */
    public boolean scope() {
        return scope;
    }

    /** 分组的消息键 */
    public String groupKey() {
        return group;
    }

    /** 名称的消息键 */
    public String labelKey() {
        return label;
    }

    public Dimension dimension() {
        return dimension;
    }

    /** 没有设置时的默认科目编码 */
    public String defaultCode() {
        return defaultCode;
    }

    public Map<String, String> dimensionDefaults() {
        return dimensionDefaults;
    }

    /** 没有任何设置时改用的用途（如其他出入库的对方科目沿用待处理财产损溢的设置），没有为空 */
    public AccountRole fallback() {
        return this == OTHER_IN_OFFSET || this == OTHER_OUT_OFFSET ? PENDING_LOSS : null;
    }

    /** 某个维度取值的默认科目编码 */
    public String defaultCode(String dimValue) {
        return dimValue == null ? defaultCode : dimensionDefaults.getOrDefault(dimValue, defaultCode);
    }
}
