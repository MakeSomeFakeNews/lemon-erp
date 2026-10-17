package com.lemon.erp.modules.hr.model;

import com.lemon.erp.common.util.Decimals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 人力资源的计算规则：计薪天数、社保公积金比例、个税税率表、考勤扣款、年假（绩效等级见 PerfGrade）。
 * 个税按月计算（应纳税所得额 = 应发 - 个人社保公积金 - 5000），未实现累计预扣法和专项附加扣除
 */
public final class HrConstants {

    /** 月计薪天数：日工资 = (基本工资 + 岗位津贴) / 21.75，小时工资 = 日工资 / 8 */
    public static final BigDecimal MONTH_PAY_DAYS = new BigDecimal("21.75");
    public static final BigDecimal DAY_HOURS = BigDecimal.valueOf(8);
    /** 个人社保：养老 8% + 医疗 2% + 失业 0.5% */
    public static final BigDecimal SOCIAL_PERSONAL_RATE = new BigDecimal("0.105");
    /** 单位社保：养老 16% + 医疗 8.5% + 失业 0.5% + 工伤 0.2% + 生育 0.8% */
    public static final BigDecimal SOCIAL_COMPANY_RATE = new BigDecimal("0.26");
    /** 每次迟到 / 早退扣款 */
    public static final BigDecimal LATE_FINE = BigDecimal.valueOf(50);
    /** 病假按小时工资扣发 40% */
    public static final BigDecimal SICK_DEDUCT_RATE = new BigDecimal("0.4");
    /** 加班费倍数：工作日 1.5、休息日 2、法定节假日 3 */
    public static final BigDecimal OVERTIME_WORKDAY = new BigDecimal("1.5");
    public static final BigDecimal OVERTIME_RESTDAY = BigDecimal.valueOf(2);
    public static final BigDecimal OVERTIME_HOLIDAY = BigDecimal.valueOf(3);
    /** 个税起征点（月） */
    public static final BigDecimal TAX_THRESHOLD = BigDecimal.valueOf(5000);
    /** 试用期默认 3 个月 */
    public static final int PROBATION_MONTHS = 3;

    /** 月度税率表：应纳税所得额上限、税率、速算扣除数 */
    private static final long[][] TAX_TABLE = {
            {3000, 3, 0}, {12000, 10, 210}, {25000, 20, 1410}, {35000, 25, 2660},
            {55000, 30, 4410}, {80000, 35, 7160}, {Long.MAX_VALUE, 45, 15160}};

    /** 累计预扣法预扣率表（按本年累计预扣预缴应纳税所得额）：上限、预扣率、速算扣除数 */
    private static final long[][] ANNUAL_TAX_TABLE = {
            {36000, 3, 0}, {144000, 10, 2520}, {300000, 20, 16920}, {420000, 25, 31920},
            {660000, 30, 52920}, {960000, 35, 85920}, {Long.MAX_VALUE, 45, 181920}};

    private HrConstants() {
    }

    /** 累计预扣的个税（本年累计应纳税额），累计应纳税所得额不大于 0 时为 0 */
    public static BigDecimal cumulativeTax(BigDecimal cumTaxable) {
        if (cumTaxable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        for (long[] row : ANNUAL_TAX_TABLE) {
            if (cumTaxable.compareTo(BigDecimal.valueOf(row[0])) <= 0) {
                return cumTaxable.multiply(BigDecimal.valueOf(row[1])).divide(Decimals.HUNDRED)
                        .subtract(BigDecimal.valueOf(row[2])).setScale(2, RoundingMode.HALF_UP);
            }
        }
        throw new IllegalStateException();
    }

    /** 个人所得税（月），应纳税所得额不大于 0 时为 0 */
    public static BigDecimal incomeTax(BigDecimal taxable) {
        if (taxable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        for (long[] row : TAX_TABLE) {
            if (taxable.compareTo(BigDecimal.valueOf(row[0])) <= 0) {
                return taxable.multiply(BigDecimal.valueOf(row[1])).divide(Decimals.HUNDRED)
                        .subtract(BigDecimal.valueOf(row[2])).setScale(2, RoundingMode.HALF_UP);
            }
        }
        throw new IllegalStateException();
    }

    /** 法定年假：累计工作满 1 年不满 10 年 5 天，满 10 年不满 20 年 10 天，满 20 年 15 天 */
    public static int annualLeaveDays(LocalDate workStart, LocalDate asOf) {
        long years = ChronoUnit.YEARS.between(workStart, asOf);
        return years < 1 ? 0 : years < 10 ? 5 : years < 20 ? 10 : 15;
    }
}
