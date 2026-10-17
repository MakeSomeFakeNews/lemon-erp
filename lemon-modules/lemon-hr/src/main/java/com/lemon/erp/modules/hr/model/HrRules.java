package com.lemon.erp.modules.hr.model;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.rule.BizRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 人力资源的计算规则（按租户的业务参数，「系统管理 → 业务参数 → 人力资源」修改）：计薪天数、每日工时、默认社保比例、
 * 考勤扣款、午休扣除、加班倍数、个税减除费用与累计预扣率表、试用期。比例类参数按百分数保存（10.5 表示 10.5%）。
 * 未修改的参数使用 {@link #DEFAULTS}（即 {@link #DEFAULT}）
 *
 * @param monthPayDays    月计薪天数：日工资 = (基本工资 + 岗位津贴) / 月计薪天数
 * @param dayHours        每日工时：小时工资 = 日工资 / 每日工时；请假跨天时每天按此计
 * @param socialPersonal  个人社保比例（%），员工没有社保方案时使用
 * @param socialCompany   单位社保比例（%），员工没有社保方案时使用
 * @param lateFine        每次迟到 / 早退扣款
 * @param sickDeduct      病假按小时工资扣发的比例（%）
 * @param overtimeWorkday 工作日加班倍数
 * @param overtimeRestday 休息日加班倍数
 * @param overtimeHoliday 法定节假日加班倍数
 * @param taxThreshold    个税基本减除费用（月）
 * @param taxBrackets     累计预扣率表，按上限从小到大，最后一档不限
 * @param probationMonths 默认试用期（月）
 * @param lunchThreshold  出勤超过该小时数时扣除午休（不含等于）
 * @param lunchBreak      扣除的午休小时数
 */
public record HrRules(BigDecimal monthPayDays, BigDecimal dayHours, BigDecimal socialPersonal, BigDecimal socialCompany,
                      BigDecimal lateFine, BigDecimal sickDeduct, BigDecimal overtimeWorkday, BigDecimal overtimeRestday,
                      BigDecimal overtimeHoliday, BigDecimal taxThreshold, List<TaxBracket> taxBrackets, int probationMonths,
                      BigDecimal lunchThreshold, BigDecimal lunchBreak) {

    public static final String MONTH_PAY_DAYS = "hr.pay.monthDays";
    public static final String DAY_HOURS = "hr.pay.dayHours";
    public static final String SOCIAL_PERSONAL = "hr.social.personalRate";
    public static final String SOCIAL_COMPANY = "hr.social.companyRate";
    public static final String LATE_FINE = "hr.attendance.lateFine";
    public static final String SICK_DEDUCT = "hr.leave.sickDeductRate";
    public static final String OVERTIME_WORKDAY = "hr.overtime.workdayRate";
    public static final String OVERTIME_RESTDAY = "hr.overtime.restdayRate";
    public static final String OVERTIME_HOLIDAY = "hr.overtime.holidayRate";
    public static final String TAX_THRESHOLD = "hr.tax.threshold";
    public static final String TAX_TABLE = "hr.tax.table";
    public static final String PROBATION_MONTHS = "hr.employee.probationMonths";
    public static final String LUNCH_THRESHOLD = "hr.attendance.lunchThresholdHours";
    public static final String LUNCH_BREAK = "hr.attendance.lunchBreakHours";

    /** 默认值（规范写法，业务参数的默认值）：与此前写死的规则一致 */
    public static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        // 日工资 = (基本工资 + 岗位津贴) / 21.75，小时工资 = 日工资 / 8
        m.put(MONTH_PAY_DAYS, "21.75");
        m.put(DAY_HOURS, "8");
        // 个人：养老 8% + 医疗 2% + 失业 0.5%；单位：养老 16% + 医疗 8.5% + 失业 0.5% + 工伤 0.2% + 生育 0.8%
        m.put(SOCIAL_PERSONAL, "10.5");
        m.put(SOCIAL_COMPANY, "26");
        m.put(LATE_FINE, "50");
        m.put(SICK_DEDUCT, "40");
        m.put(OVERTIME_WORKDAY, "1.5");
        m.put(OVERTIME_RESTDAY, "2");
        m.put(OVERTIME_HOLIDAY, "3");
        m.put(TAX_THRESHOLD, "5000");
        // 综合所得个人所得税预扣率表（居民个人工资薪金累计预扣法）：累计预扣预缴应纳税所得额上限、预扣率（%）、速算扣除数
        m.put(TAX_TABLE, "36000,3,0;144000,10,2520;300000,20,16920;420000,25,31920;660000,30,52920;960000,35,85920;,45,181920");
        m.put(PROBATION_MONTHS, "3");
        // 出勤超过 5 小时扣除 1 小时午休
        m.put(LUNCH_THRESHOLD, "5");
        m.put(LUNCH_BREAK, "1");
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    public static final HrRules DEFAULT = of(DEFAULTS::get);

    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    /**
     * 预扣率表的一档
     *
     * @param upper     累计应纳税所得额上限（含），null 表示不限
     * @param rate      预扣率（%）
     * @param deduction 速算扣除数
     */
    public record TaxBracket(BigDecimal upper, BigDecimal rate, BigDecimal deduction) {
    }

    /** 由参数值构造（参数标识 → 值） */
    public static HrRules of(Function<String, String> values) {
        return new HrRules(dec(values, MONTH_PAY_DAYS), dec(values, DAY_HOURS), dec(values, SOCIAL_PERSONAL), dec(values, SOCIAL_COMPANY),
                dec(values, LATE_FINE), dec(values, SICK_DEDUCT), dec(values, OVERTIME_WORKDAY), dec(values, OVERTIME_RESTDAY),
                dec(values, OVERTIME_HOLIDAY), dec(values, TAX_THRESHOLD), brackets(values.apply(TAX_TABLE)),
                Integer.parseInt(values.apply(PROBATION_MONTHS)), dec(values, LUNCH_THRESHOLD), dec(values, LUNCH_BREAK));
    }

    /** 解析预扣率表：每行 上限,预扣率,速算扣除数，最后一行上限留空 */
    public static List<TaxBracket> brackets(String value) {
        List<TaxBracket> list = new ArrayList<>();
        for (List<BigDecimal> row : BizRule.parseTable(value)) {
            list.add(new TaxBracket(row.get(0), row.get(1), row.get(2)));
        }
        return List.copyOf(list);
    }

    private static BigDecimal dec(Function<String, String> values, String key) {
        return new BigDecimal(values.apply(key));
    }

    /** 个人社保比例（小数，如 0.105） */
    public BigDecimal socialPersonalRate() {
        return socialPersonal.divide(Decimals.HUNDRED);
    }

    /** 单位社保比例（小数，如 0.26） */
    public BigDecimal socialCompanyRate() {
        return socialCompany.divide(Decimals.HUNDRED);
    }

    /** 病假扣发比例（小数，如 0.4） */
    public BigDecimal sickDeductRate() {
        return sickDeduct.divide(Decimals.HUNDRED);
    }

    /** 月计薪小时数 = 月计薪天数 × 每日工时（默认 174） */
    public BigDecimal monthPayHours() {
        return monthPayDays.multiply(dayHours);
    }

    /**
     * 出勤时长（小时，两位小数）：打卡间隔超过午休扣除阈值（默认 5 小时）时扣除午休（默认 1 小时），不小于 0
     *
     * @param minutes 上下班打卡间隔（分钟）
     */
    public BigDecimal workHours(long minutes) {
        BigDecimal net = BigDecimal.valueOf(minutes);
        if (net.compareTo(lunchThreshold.multiply(SIXTY)) > 0) {
            net = net.subtract(lunchBreak.multiply(SIXTY)).max(BigDecimal.ZERO);
        }
        return net.divide(SIXTY, 2, RoundingMode.HALF_UP);
    }

    /** 累计预扣的个税（本年累计应纳税额）= 累计应纳税所得额 × 预扣率 − 速算扣除数，累计应纳税所得额不大于 0 时为 0 */
    public BigDecimal cumulativeTax(BigDecimal cumTaxable) {
        if (cumTaxable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        for (TaxBracket b : taxBrackets) {
            if (b.upper() == null || cumTaxable.compareTo(b.upper()) <= 0) {
                return cumTaxable.multiply(b.rate()).divide(Decimals.HUNDRED)
                        .subtract(b.deduction()).setScale(2, RoundingMode.HALF_UP);
            }
        }
        throw new IllegalStateException();
    }
}
