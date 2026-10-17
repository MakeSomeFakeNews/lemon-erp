package com.lemon.erp.modules.hr.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 人力资源的法定规则：年假（绩效等级见 PerfGrade）。
 * 计薪天数、社保比例、个税税率表、考勤扣款、加班倍数等按租户可配置，见 {@link HrRules}
 */
public final class HrConstants {

    private HrConstants() {
    }

    /** 法定年假：累计工作满 1 年不满 10 年 5 天，满 10 年不满 20 年 10 天，满 20 年 15 天 */
    public static int annualLeaveDays(LocalDate workStart, LocalDate asOf) {
        long years = ChronoUnit.YEARS.between(workStart, asOf);
        return years < 1 ? 0 : years < 10 ? 5 : years < 20 ? 10 : 15;
    }
}
