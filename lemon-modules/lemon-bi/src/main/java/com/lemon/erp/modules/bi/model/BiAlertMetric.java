package com.lemon.erp.modules.bi.model;

/**
 * 预警规则可以监控的指标（取值 SQL 见 BiInsightMapper.selectMetric，预算超支由财务预算执行计算）
 */
public enum BiAlertMetric {

    AR_OVERDUE("bi.label.overdueReceivablesAmount", "bi.label.cny"),
    STOCK_BELOW_SAFETY("bi.label.materialsBelowSafetyStock", "bi.label.items"),
    SLOW_MOVING("bi.label.slowMovingMaterialsNoOutboundDays", "bi.label.items"),
    GROSS_MARGIN("bi.label.grossMarginMonth", "bi.label.percent"),
    TICKET_OVERDUE("bi.label.overdueUnresolvedServiceTickets", "bi.label.docs"),
    TASK_OVERDUE("bi.label.overdueProjectTasks", "bi.label.items2"),
    WO_LATE("bi.label.lateWorkOrders", "bi.label.docs"),
    BUDGET_OVERRUN("bi.label.accountsOverBudgetYtd", "bi.label.items2");

    /** 名称、单位的消息键，输出时按请求语言取消息 */
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
