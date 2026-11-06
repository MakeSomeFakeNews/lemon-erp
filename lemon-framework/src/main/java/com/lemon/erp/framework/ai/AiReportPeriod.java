package com.lemon.erp.framework.ai;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 智能报告的统计期间（含首尾两天）
 *
 * @param start 开始日期
 * @param end   结束日期（含）
 */
public record AiReportPeriod(LocalDate start, LocalDate end) {

    public AiReportPeriod {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("invalid period " + start + " ~ " + end);
        }
    }

    /** 日期所在的自然周（周一至周日） */
    public static AiReportPeriod weekOf(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new AiReportPeriod(monday, monday.plusDays(6));
    }

    public static AiReportPeriod month(YearMonth month) {
        return new AiReportPeriod(month.atDay(1), month.atEndOfMonth());
    }

    /** 天数 */
    public long days() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    /** 紧挨着的上一个同样长度的期间（整月按上一个整月），用于环比 */
    public AiReportPeriod previous() {
        if (start.getDayOfMonth() == 1 && end.equals(YearMonth.from(end).atEndOfMonth()) && YearMonth.from(start).equals(YearMonth.from(end))) {
            return month(YearMonth.from(start).minusMonths(1));
        }
        return new AiReportPeriod(start.minusDays(days()), start.minusDays(1));
    }
}
