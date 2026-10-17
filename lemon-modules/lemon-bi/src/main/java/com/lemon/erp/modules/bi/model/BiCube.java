package com.lemon.erp.modules.bi.model;

import java.util.List;

/**
 * 多维分析的数据集（分析主题）：维度、指标的元数据。查询 SQL 在 BiOlapMapper.xml 中按维度编码选择表达式，
 * 编码只能是这里列出的值（白名单），不会拼接任意 SQL
 *
 * @param dateFilter 日期筛选方式：DATE 按业务日期，PERIOD 按月份，SNAPSHOT 按快照日期（不筛选时取最新快照）
 */
public enum BiCube {

    SALES("销售分析", "DATE", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("customer", "客户", "customer", null),  // i18n-ignore 输出处翻译
            new BiDim("owner", "销售负责人", "user", null),  // i18n-ignore 输出处翻译
            new BiDim("material", "物料", "material", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "物料分类", null, "wms_material_category"),  // i18n-ignore 输出处翻译
            new BiDim("warehouse", "仓库", "warehouse", null),  // i18n-ignore 输出处翻译
            new BiDim("dept", "部门", "dept", null),  // i18n-ignore 输出处翻译
            new BiDim("level", "客户等级", null, "scm_customer_level"),  // i18n-ignore 输出处翻译
            new BiDim("industry", "客户行业", null, "crm_industry")),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("qty", "数量", "QTY"), BiMeasure.sum("amount", "销售额", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("cost", "销售成本", "MONEY"), BiMeasure.sum("profit", "毛利", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("margin", "毛利率", "profit", "amount"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("docs", "出库单数", "COUNT"), BiMeasure.sum("customers", "客户数", "COUNT"))),  // i18n-ignore 输出处翻译

    PURCHASE("采购分析", "DATE", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("supplier", "供应商", "supplier", null),  // i18n-ignore 输出处翻译
            new BiDim("material", "物料", "material", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "物料分类", null, "wms_material_category"),  // i18n-ignore 输出处翻译
            new BiDim("warehouse", "仓库", "warehouse", null)),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("qty", "数量", "QTY"), BiMeasure.sum("amount", "采购额", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("avgPrice", "平均单价", "amount", "qty"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("docs", "入库单数", "COUNT"), BiMeasure.sum("suppliers", "供应商数", "COUNT"))),  // i18n-ignore 输出处翻译

    INVENTORY("库存分析", "SNAPSHOT", List.of(  // i18n-ignore 输出处翻译
            new BiDim("snapDate", "快照日期", null, null),  // i18n-ignore 输出处翻译
            new BiDim("warehouse", "仓库", "warehouse", null),  // i18n-ignore 输出处翻译
            new BiDim("material", "物料", "material", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "物料分类", null, "wms_material_category")),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("qty", "库存数量", "QTY"), BiMeasure.sum("amount", "库存金额", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("materials", "物料种数", "COUNT"))),  // i18n-ignore 输出处翻译

    PRODUCTION("生产分析", "PERIOD", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("material", "产品", "material", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "物料分类", null, "wms_material_category"),  // i18n-ignore 输出处翻译
            new BiDim("status", "工单状态", null, "mes_wo_status"),  // i18n-ignore 输出处翻译
            new BiDim("dept", "部门", "dept", null)),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("orders", "工单数", "COUNT"), BiMeasure.sum("planQty", "计划数量", "QTY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("finishedQty", "完工数量", "QTY"), BiMeasure.sum("scrapQty", "报废数量", "QTY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("hours", "报工工时", "QTY"), BiMeasure.sum("lateOrders", "延期工单", "COUNT"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("completion", "完工率", "finishedQty", "planQty"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("scrapRate", "报废率", "scrapQty", "finishedQty"))),  // i18n-ignore 输出处翻译

    FINANCE("财务分析", "PERIOD", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("account", "会计科目", "account", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "科目类别", null, "fin_account_category"),  // i18n-ignore 输出处翻译
            new BiDim("dept", "部门", "dept", null)),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("debit", "借方发生额", "MONEY"), BiMeasure.sum("credit", "贷方发生额", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("net", "借贷差额", "MONEY"))),  // i18n-ignore 输出处翻译

    HR("人力分析", "PERIOD", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("dept", "部门", "dept", null),  // i18n-ignore 输出处翻译
            new BiDim("category", "岗位类别", null, "hr_position_category")),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("headcount", "月末人数（按月累加）", "COUNT"), BiMeasure.sum("hires", "入职", "COUNT"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("leaves", "离职", "COUNT"), BiMeasure.ratio("turnover", "离职率", "leaves", "headcount"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("grossPay", "应发工资", "MONEY"), BiMeasure.sum("laborCost", "人工成本（含单位社保公积金）", "MONEY"))),  // i18n-ignore 输出处翻译

    CRM("客户经营分析", "PERIOD", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("owner", "负责人", "user", null),  // i18n-ignore 输出处翻译
            new BiDim("dept", "部门", "dept", null)),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("leads", "新增线索", "COUNT"), BiMeasure.sum("followups", "跟进次数", "COUNT"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("opps", "新增商机", "COUNT"), BiMeasure.sum("won", "赢单", "COUNT"), BiMeasure.sum("lost", "输单", "COUNT"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("winRate", "赢单率", "won", "closed"), BiMeasure.sum("wonAmount", "赢单金额", "MONEY"))),  // i18n-ignore 输出处翻译

    PROJECT("项目分析", "PERIOD", List.of(  // i18n-ignore 输出处翻译
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("project", "项目", "project", null),  // i18n-ignore 输出处翻译
            new BiDim("dept", "部门", "dept", null),  // i18n-ignore 输出处翻译
            new BiDim("manager", "项目经理", "user", null)),  // i18n-ignore 输出处翻译
            List.of(BiMeasure.sum("hours", "工时", "QTY"), BiMeasure.sum("laborCost", "人工成本", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("expense", "费用（含领料）", "MONEY"), BiMeasure.sum("revenue", "确认收入", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.sum("cost", "结转成本", "MONEY"), BiMeasure.sum("profit", "毛利", "MONEY"),  // i18n-ignore 输出处翻译
                    BiMeasure.ratio("margin", "毛利率", "profit", "revenue")));  // i18n-ignore 输出处翻译

    private final String label;
    private final String dateFilter;
    private final List<BiDim> dims;
    private final List<BiMeasure> measures;

    BiCube(String label, String dateFilter, List<BiDim> dims, List<BiMeasure> measures) {
        this.label = label;
        this.dateFilter = dateFilter;
        this.dims = dims;
        this.measures = measures;
    }

    public String getLabel() {
        return label;
    }

    public String getDateFilter() {
        return dateFilter;
    }

    public List<BiDim> getDims() {
        return dims;
    }

    public List<BiMeasure> getMeasures() {
        return measures;
    }

    public BiDim dim(String key) {
        return dims.stream().filter(d -> d.key().equals(key)).findFirst().orElse(null);
    }

    /**
     * 维度
     *
     * @param labelSource 维度值为 ID 时名称的来源（customer、supplier、user、material、warehouse、dept、account、project），为空不转换
     * @param dictType    维度值为字典值时的字典类型，由前端转换名称
     */
    public record BiDim(String key, String label, String labelSource, String dictType) {
        static final BiDim YEAR = new BiDim("year", "年", null, null);  // i18n-ignore 输出处翻译
        static final BiDim QUARTER = new BiDim("quarter", "季度", null, null);  // i18n-ignore 输出处翻译
        static final BiDim PERIOD = new BiDim("period", "月份", null, null);  // i18n-ignore 输出处翻译
    }

    /**
     * 指标：SUM 为数据库汇总的可加指标；RATIO = numerator / denominator（百分比或单价），按汇总后的值计算
     *
     * @param format MONEY 金额、QTY 数量、COUNT 计数、PERCENT 百分比、PRICE 单价
     */
    public record BiMeasure(String key, String label, String format, String numerator, String denominator) {

        static BiMeasure sum(String key, String label, String format) {
            return new BiMeasure(key, label, format, null, null);
        }

        static BiMeasure ratio(String key, String label, String numerator, String denominator) {
            return new BiMeasure(key, label, "avgPrice".equals(key) ? "PRICE" : "PERCENT", numerator, denominator);
        }

        public boolean isRatio() {
            return numerator != null;
        }
    }
}
