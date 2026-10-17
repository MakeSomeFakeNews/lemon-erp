package com.lemon.erp.modules.bi.model;

/**
 * 预警规则可以监控的指标（取值 SQL 见 BiInsightMapper.selectMetric，预算超支由财务预算执行计算）
 */
public enum BiAlertMetric {

    AR_OVERDUE("应收账款逾期金额", "元"),  // i18n-ignore 输出处翻译
    STOCK_BELOW_SAFETY("低于安全库存的物料数", "种"),  // i18n-ignore 输出处翻译
    SLOW_MOVING("呆滞物料数（90 天无出库）", "种"),  // i18n-ignore 输出处翻译
    GROSS_MARGIN("本月销售毛利率", "%"),  // i18n-ignore 输出处翻译
    TICKET_OVERDUE("超时未解决的服务工单", "张"),  // i18n-ignore 输出处翻译
    TASK_OVERDUE("逾期未完成的项目任务", "个"),  // i18n-ignore 输出处翻译
    WO_LATE("延期未完工的生产工单", "张"),  // i18n-ignore 输出处翻译
    BUDGET_OVERRUN("本年累计超预算的科目数", "个");  // i18n-ignore 输出处翻译

    private final String label;
    private final String unit;

    BiAlertMetric(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public static BiAlertMetric of(String name) {
        for (BiAlertMetric m : values()) {
            if (m.name().equals(name)) {
                return m;
            }
        }
        return null;
    }
}
