package com.lemon.erp.modules.fin.model;

import java.util.Map;

/**
 * 自动凭证科目：业务单据生成凭证时各用途使用的科目。
 * 默认取 init.sql 预置科目的编码，可在「凭证科目设置」中改为其他末级科目；带维度的用途可以再按物料分类 / 职位类别分别指定
 */
public enum AccountRole {

    RECEIVABLE("往来", "应收账款", "1122"),
    PAYABLE("往来", "应付账款", "2202"),
    ADVANCE_RECEIVED("往来", "预收账款", "2203"),
    PREPAYMENT("往来", "预付账款", "1123"),
    NOTES_RECEIVABLE("往来", "应收票据", "1121"),
    NOTES_PAYABLE("往来", "应付票据", "2201"),
    OTHER_RECEIVABLE("往来", "其他应收款（员工借款）", "1221"),
    OTHER_PAYABLE("往来", "其他应付款（费用、代扣款）", "2241"),

    /** 默认：低值易耗品 → 周转材料，半成品、成品 → 库存商品，其余 → 原材料 */
    INVENTORY("存货与成本", "存货", "1403", Dimension.MATERIAL_CATEGORY,
            Map.of(FinConstants.LOW_VALUE_CATEGORY, "1411", FinConstants.CATEGORY_SEMI, "1405", FinConstants.CATEGORY_FINISHED, "1405")),
    OUTSOURCE_MATERIAL("存货与成本", "委托加工物资", "1408"),
    PRODUCTION_COST("存货与成本", "生产成本", "5001"),
    MANUFACTURING_OVERHEAD("存货与成本", "制造费用", "5101"),
    CONTRACT_COST("存货与成本", "合同履约成本（项目）", "5401"),
    PENDING_LOSS("存货与成本", "待处理财产损溢（盘盈盘亏）", "1901"),

    REVENUE("收入与费用", "主营业务收入", "6001", Dimension.MATERIAL_CATEGORY),
    COST_OF_SALES("收入与费用", "主营业务成本", "6401", Dimension.MATERIAL_CATEGORY),
    OTHER_REVENUE("收入与费用", "其他业务收入（服务收费）", "6051"),
    CONTRACT_LIABILITY("收入与费用", "合同负债（项目开票）", "2205"),
    SELLING_EXPENSE("收入与费用", "销售费用（运费、售后备件）", "6601"),
    TRAINING_EXPENSE("收入与费用", "培训费", "660205"),
    FINANCE_EXPENSE("收入与费用", "财务费用（手续费、贴现、汇兑损益）", "6603"),
    NON_OPERATING_INCOME("收入与费用", "营业外收入", "6301"),
    NON_OPERATING_EXPENSE("收入与费用", "营业外支出", "6711"),

    INPUT_VAT("税费", "进项税额", "222101"),
    OUTPUT_VAT("税费", "销项税额", "222102"),
    UNPAID_VAT("税费", "未交增值税", "222104"),
    CITY_TAX("税费", "应交城市维护建设税", "222105"),
    EDU_SURCHARGE("税费", "应交教育费附加", "222106"),
    LOCAL_EDU_SURCHARGE("税费", "应交地方教育附加", "222107"),
    TAX_SURCHARGE("税费", "税金及附加", "6403"),
    INCOME_TAX("税费", "应交个人所得税", "222103"),

    FIXED_ASSET("资产", "固定资产", "1601"),
    DEPRECIATION("资产", "累计折旧", "1602"),
    ASSET_DISPOSAL("资产", "固定资产清理", "1606"),

    SALARY("薪酬", "应付职工薪酬", "2211"),
    /** 默认：生产岗位 → 制造费用（成本核算时按工时分配到工单），销售岗位 → 销售费用，其余 → 管理费用-工资 */
    SALARY_EXPENSE("薪酬", "工资费用", "660201", Dimension.POSITION_CATEGORY, Map.of("PRODUCTION", "5101", "SALES", "6601")),

    PROFIT_THIS_YEAR("期末", "本年利润", "4103"),
    PROFIT_DISTRIBUTION("期末", "利润分配", "4104");

    /** 用途可以按什么分别指定科目 */
    public enum Dimension {
        NONE(null),
        /** 字典 wms_material_category */
        MATERIAL_CATEGORY("wms_material_category"),
        /** 字典 hr_position_category */
        POSITION_CATEGORY("hr_position_category");

        private final String dictType;

        Dimension(String dictType) {
            this.dictType = dictType;
        }

        public String dictType() {
            return dictType;
        }
    }

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

    public String group() {
        return group;
    }

    public String label() {
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

    /** 某个维度取值的默认科目编码 */
    public String defaultCode(String dimValue) {
        return dimValue == null ? defaultCode : dimensionDefaults.getOrDefault(dimValue, defaultCode);
    }
}
