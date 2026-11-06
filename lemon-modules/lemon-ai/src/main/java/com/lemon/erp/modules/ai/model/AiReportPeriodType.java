package com.lemon.erp.modules.ai.model;

import com.lemon.erp.framework.ai.AiReportPeriod;

import java.time.LocalDate;
import java.time.YearMonth;

/** 智能报告的期间类型：周报（周一至周日）、月报（自然月） */
public enum AiReportPeriodType {
    WEEK("ai.enum.reportPeriod.WEEK"),
    MONTH("ai.enum.reportPeriod.MONTH");

    private final String labelKey;

    AiReportPeriodType(String labelKey) {
        this.labelKey = labelKey;
    }

    /** 名称的消息键（周报 / 月报） */
    public String labelKey() {
        return labelKey;
    }

    /** 日期所在的期间 */
    public AiReportPeriod period(LocalDate date) {
        return this == WEEK ? AiReportPeriod.weekOf(date) : AiReportPeriod.month(YearMonth.from(date));
    }

    /** 上一个完整的期间（定时任务：周一生成上周的周报，1 日生成上月的月报） */
    public AiReportPeriod last(LocalDate today) {
        return period(this == WEEK ? today.minusWeeks(1) : today.minusMonths(1));
    }
}
