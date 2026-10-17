package com.lemon.erp.modules.bi.model;

import java.util.List;

/**
 * 多维分析的数据集（分析主题）：维度、指标的元数据。查询 SQL 在 BiOlapMapper.xml 中按维度编码选择表达式，
 * 编码只能是这里列出的值（白名单），不会拼接任意 SQL
 *
 * @param dateFilter 日期筛选方式：DATE 按业务日期，PERIOD 按月份，SNAPSHOT 按快照日期（不筛选时取最新快照）
 */
public enum BiCube {

    SALES("bi.label.salesAnalysis", "DATE", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("customer", "bi.label.customer", "customer", null),
            new BiDim("owner", "bi.label.salesRep", "user", null),
            new BiDim("material", "bi.label.material", "material", null),
            new BiDim("category", "bi.label.materialCategory", null, "wms_material_category"),
            new BiDim("warehouse", "bi.label.warehouse2", "warehouse", null),
            new BiDim("dept", "bi.label.dept", "dept", null),
            new BiDim("level", "bi.label.customerLevel", null, "scm_customer_level"),
            new BiDim("industry", "bi.label.customerIndustry", null, "crm_industry")),
            List.of(BiMeasure.sum("qty", "bi.label.quantity", "QTY"), BiMeasure.sum("amount", "bi.label.salesAmount", "MONEY"),
                    BiMeasure.sum("cost", "bi.label.costOfSales", "MONEY"), BiMeasure.sum("profit", "bi.label.grossProfit", "MONEY"),
                    BiMeasure.ratio("margin", "bi.label.grossMargin", "profit", "amount"),
                    BiMeasure.sum("docs", "bi.label.outboundDocs", "COUNT"), BiMeasure.sum("customers", "bi.label.customers", "COUNT"))),

    PURCHASE("bi.label.purchaseAnalysis", "DATE", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("supplier", "bi.label.supplier", "supplier", null),
            new BiDim("material", "bi.label.material", "material", null),
            new BiDim("category", "bi.label.materialCategory", null, "wms_material_category"),
            new BiDim("warehouse", "bi.label.warehouse2", "warehouse", null)),
            List.of(BiMeasure.sum("qty", "bi.label.quantity", "QTY"), BiMeasure.sum("amount", "bi.label.purchaseAmount", "MONEY"),
                    BiMeasure.ratio("avgPrice", "bi.label.averagePrice", "amount", "qty"),
                    BiMeasure.sum("docs", "bi.label.inboundDocs", "COUNT"), BiMeasure.sum("suppliers", "bi.label.suppliers", "COUNT"))),

    INVENTORY("bi.label.inventoryAnalysis", "SNAPSHOT", List.of(
            new BiDim("snapDate", "bi.label.snapshotDate", null, null),
            new BiDim("warehouse", "bi.label.warehouse2", "warehouse", null),
            new BiDim("material", "bi.label.material", "material", null),
            new BiDim("category", "bi.label.materialCategory", null, "wms_material_category")),
            List.of(BiMeasure.sum("qty", "bi.label.stockQuantity", "QTY"), BiMeasure.sum("amount", "bi.label.stockValue", "MONEY"),
                    BiMeasure.sum("materials", "bi.label.materials", "COUNT"))),

    PRODUCTION("bi.label.productionAnalysis", "PERIOD", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("material", "bi.label.product", "material", null),
            new BiDim("category", "bi.label.materialCategory", null, "wms_material_category"),
            new BiDim("status", "bi.label.workOrderStatus", null, "mes_wo_status"),
            new BiDim("dept", "bi.label.dept", "dept", null)),
            List.of(BiMeasure.sum("orders", "bi.label.workOrders", "COUNT"), BiMeasure.sum("planQty", "bi.label.plannedQty", "QTY"),
                    BiMeasure.sum("finishedQty", "bi.label.completedQty", "QTY"), BiMeasure.sum("scrapQty", "bi.label.scrapQty", "QTY"),
                    BiMeasure.sum("hours", "bi.label.reportedHours", "QTY"), BiMeasure.sum("lateOrders", "bi.label.lateWorkOrders2", "COUNT"),
                    BiMeasure.ratio("completion", "bi.label.completionRate", "finishedQty", "planQty"),
                    BiMeasure.ratio("scrapRate", "bi.label.scrapRate", "scrapQty", "finishedQty"))),

    FINANCE("bi.label.financialAnalysis", "PERIOD", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("account", "bi.label.account", "account", null),
            new BiDim("category", "bi.label.accountCategory", null, "fin_account_category"),
            new BiDim("dept", "bi.label.dept", "dept", null)),
            List.of(BiMeasure.sum("debit", "bi.label.debitAmount", "MONEY"), BiMeasure.sum("credit", "bi.label.creditAmount", "MONEY"),
                    BiMeasure.sum("net", "bi.label.netDebitCredit", "MONEY"))),

    HR("bi.label.hrAnalysis", "PERIOD", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("dept", "bi.label.dept", "dept", null),
            new BiDim("category", "bi.label.positionCategory", null, "hr_position_category")),
            List.of(BiMeasure.sum("headcount", "bi.label.monthEndHeadcountSummedMonth", "COUNT"), BiMeasure.sum("hires", "bi.label.hires", "COUNT"),
                    BiMeasure.sum("leaves", "bi.label.leavers", "COUNT"), BiMeasure.ratio("turnover", "bi.label.turnoverRate", "leaves", "headcount"),
                    BiMeasure.sum("grossPay", "bi.label.grossPay", "MONEY"), BiMeasure.sum("laborCost", "bi.label.laborCostInclEmployerSocialInsurance", "MONEY"))),

    CRM("bi.label.customerAnalysis", "PERIOD", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("owner", "bi.label.owner", "user", null),
            new BiDim("dept", "bi.label.dept", "dept", null)),
            List.of(BiMeasure.sum("leads", "bi.label.newLeads", "COUNT"), BiMeasure.sum("followups", "bi.label.followUps", "COUNT"),
                    BiMeasure.sum("opps", "bi.label.newOpportunities", "COUNT"), BiMeasure.sum("won", "bi.label.won", "COUNT"), BiMeasure.sum("lost", "bi.label.lost", "COUNT"),
                    BiMeasure.ratio("winRate", "bi.label.winRate", "won", "closed"), BiMeasure.sum("wonAmount", "bi.label.wonAmount", "MONEY"))),

    PROJECT("bi.label.projectAnalysis", "PERIOD", List.of(
            BiDim.YEAR, BiDim.QUARTER, BiDim.PERIOD,
            new BiDim("project", "bi.label.project", "project", null),
            new BiDim("dept", "bi.label.dept", "dept", null),
            new BiDim("manager", "bi.label.projectManager", "user", null)),
            List.of(BiMeasure.sum("hours", "bi.label.hours", "QTY"), BiMeasure.sum("laborCost", "bi.label.laborCost", "MONEY"),
                    BiMeasure.sum("expense", "bi.label.expensesInclMaterials", "MONEY"), BiMeasure.sum("revenue", "bi.label.recognizedRevenue", "MONEY"),
                    BiMeasure.sum("cost", "bi.label.recognizedCost", "MONEY"), BiMeasure.sum("profit", "bi.label.grossProfit", "MONEY"),
                    BiMeasure.ratio("margin", "bi.label.grossMargin", "profit", "revenue")));

    /** 名称的消息键（维度、指标的 label 同样是消息键），输出时按请求语言取消息 */
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
        static final BiDim YEAR = new BiDim("year", "bi.label.year", null, null);
        static final BiDim QUARTER = new BiDim("quarter", "bi.label.quarter", null, null);
        static final BiDim PERIOD = new BiDim("period", "bi.label.month", null, null);
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
