package com.lemon.erp.modules.fin.model;

import java.util.Map;

/**
 * 自动凭证科目：业务单据生成凭证时各用途使用的科目。
 * 默认取 init.sql 预置科目的编码，可在「凭证科目设置」中改为其他末级科目；带维度的用途可以再按物料分类 / 职位类别 / 收发类别分别指定
 */
public enum AccountRole {

    RECEIVABLE("fin.label.receivablesPayables", "fin.label.accountsReceivable", "1122"),
    PAYABLE("fin.label.receivablesPayables", "fin.label.accountsPayable", "2202"),
    ADVANCE_RECEIVED("fin.label.receivablesPayables", "fin.label.advancesCustomers", "2203"),
    PREPAYMENT("fin.label.receivablesPayables", "fin.label.prepayments", "1123"),
    NOTES_RECEIVABLE("fin.label.receivablesPayables", "fin.label.notesReceivable", "1121"),
    NOTES_PAYABLE("fin.label.receivablesPayables", "fin.label.notesPayable", "2201"),
    OTHER_RECEIVABLE("fin.label.receivablesPayables", "fin.label.otherReceivablesEmployeeLoans", "1221"),
    OTHER_PAYABLE("fin.label.receivablesPayables", "fin.label.otherPayablesExpensesWithholdings", "2241"),

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
    TRAINING_EXPENSE("fin.label.revenueExpenses", "fin.label.trainingExpenses", "660205"),
    FINANCE_EXPENSE("fin.label.revenueExpenses", "fin.label.financialExpensesFeesDiscountsFxGains", "6603"),
    NON_OPERATING_INCOME("fin.label.revenueExpenses", "fin.label.nonOperatingIncome", "6301"),
    NON_OPERATING_EXPENSE("fin.label.revenueExpenses", "fin.label.nonOperatingExpenses", "6711"),

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

    AccountRole(String group, String label, String defaultCode) {
        this(group, label, defaultCode, Dimension.NONE, Map.of());
    }

    AccountRole(String group, String label, String defaultCode, Dimension dimension) {
        this(group, label, defaultCode, dimension, Map.of());
    }

    AccountRole(String group, String label, String defaultCode, Dimension dimension, Map<String, String> dimensionDefaults) {
        this.group = group;
        this.label = label;
        this.defaultCode = defaultCode;
        this.dimension = dimension;
        this.dimensionDefaults = dimensionDefaults;
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
