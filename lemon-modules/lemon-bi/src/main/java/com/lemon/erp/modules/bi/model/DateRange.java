package com.lemon.erp.modules.bi.model;

import com.lemon.erp.common.exception.BizException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 多维分析的相对日期范围
 */
public enum DateRange {
    /** 本月 */
    THIS_MONTH,
    /** 本季度 */
    THIS_QUARTER,
    /** 本年 */
    THIS_YEAR,
    /** 最近 12 个月 */
    LAST_12M,
    /** 全部 */
    ALL;

    /** 每季度的月数 */
    private static final int QUARTER_MONTHS = 3;
    /** 最近 12 个月：本月之前再取 11 个月 */
    private static final int LAST_12M_PREVIOUS_MONTHS = 11;

    /** 按编码取值，编码不正确时报错 */
    public static DateRange of(String code) {
        for (DateRange r : values()) {
            if (r.name().equals(code)) {
                return r;
            }
        }
        throw new BizException("bi.error.invalidDateRange", code);
    }

    /** 截至 today 的 [开始日期, 结束日期]，全部时均为空 */
    public LocalDate[] resolve(LocalDate today) {
        return switch (this) {
            case THIS_MONTH -> new LocalDate[]{today.withDayOfMonth(1), today};
            case THIS_QUARTER -> new LocalDate[]{YearMonth.of(today.getYear(),
                    (today.getMonthValue() - 1) / QUARTER_MONTHS * QUARTER_MONTHS + 1).atDay(1), today};
            case THIS_YEAR -> new LocalDate[]{today.withDayOfYear(1), today};
            case LAST_12M -> new LocalDate[]{YearMonth.from(today).minusMonths(LAST_12M_PREVIOUS_MONTHS).atDay(1), today};
            case ALL -> new LocalDate[]{null, null};
        };
    }
}
