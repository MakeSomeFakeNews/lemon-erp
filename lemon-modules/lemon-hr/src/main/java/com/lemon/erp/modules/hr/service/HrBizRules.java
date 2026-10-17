package com.lemon.erp.modules.hr.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.hr.model.HrRules;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.lemon.erp.modules.hr.model.HrRules.DEFAULTS;

/**
 * 人力资源的业务参数（模块、名称、说明为消息键），默认值见 {@link HrRules#DEFAULTS}，读取用 {@link HrRuleService}
 */
@Component
public class HrBizRules implements BizRuleProvider {

    private static final String MODULE = "hr.label.humanResources";

    @Override
    public List<BizRule> rules() {
        return List.of(
                decimal(HrRules.MONTH_PAY_DAYS, "hr.label.rule.monthPayDays", 2, 1, 31, "hr.label.rule.monthPayDaysRemark"),
                decimal(HrRules.DAY_HOURS, "hr.label.rule.dayHours", 2, 1, 24, "hr.label.rule.dayHoursRemark"),
                decimal(HrRules.SOCIAL_PERSONAL, "hr.label.rule.socialPersonal", 2, 0, 100, "hr.label.rule.socialPersonalRemark"),
                decimal(HrRules.SOCIAL_COMPANY, "hr.label.rule.socialCompany", 2, 0, 100, "hr.label.rule.socialCompanyRemark"),
                decimal(HrRules.LATE_FINE, "hr.label.rule.lateFine", 2, 0, 10000, "hr.label.rule.lateFineRemark"),
                decimal(HrRules.LUNCH_THRESHOLD, "hr.label.rule.lunchThreshold", 2, 0, 24, "hr.label.rule.lunchThresholdRemark"),
                decimal(HrRules.LUNCH_BREAK, "hr.label.rule.lunchBreak", 2, 0, 24, "hr.label.rule.lunchBreakRemark"),
                decimal(HrRules.SICK_DEDUCT, "hr.label.rule.sickDeduct", 2, 0, 100, "hr.label.rule.sickDeductRemark"),
                decimal(HrRules.OVERTIME_WORKDAY, "hr.label.rule.overtimeWorkday", 2, 0, 10, "hr.label.rule.overtimeRemark"),
                decimal(HrRules.OVERTIME_RESTDAY, "hr.label.rule.overtimeRestday", 2, 0, 10, "hr.label.rule.overtimeRemark"),
                decimal(HrRules.OVERTIME_HOLIDAY, "hr.label.rule.overtimeHoliday", 2, 0, 10, "hr.label.rule.overtimeRemark"),
                decimal(HrRules.TAX_THRESHOLD, "hr.label.rule.taxThreshold", 2, 0, 1000000, "hr.label.rule.taxThresholdRemark"),
                BizRule.table(HrRules.TAX_TABLE, MODULE, "hr.label.rule.taxTable", DEFAULTS.get(HrRules.TAX_TABLE), List.of(
                                new BizRule.Column("hr.label.rule.taxUpper", 2, 0, 100_000_000L, true),
                                new BizRule.Column("hr.label.rule.taxRate", 2, 0, 100, false),
                                new BizRule.Column("hr.label.rule.taxDeduction", 2, 0, 100_000_000L, false)),
                        "hr.label.rule.taxTableRemark"),
                BizRule.number(HrRules.PROBATION_MONTHS, MODULE, "hr.label.rule.probationMonths",
                        Integer.parseInt(DEFAULTS.get(HrRules.PROBATION_MONTHS)), 0, 12, "hr.label.rule.probationMonthsRemark"));
    }

    private static BizRule decimal(String key, String label, int scale, int min, int max, String remark) {
        return BizRule.decimal(key, MODULE, label, DEFAULTS.get(key), scale, min, max, remark);
    }

    /** 预扣率表：除最后一档外上限必填且从小到大，最后一档上限留空（不限） */
    @Override
    public void validate(String key, String value) {
        if (!HrRules.TAX_TABLE.equals(key)) {
            return;
        }
        List<HrRules.TaxBracket> brackets = HrRules.brackets(value);
        for (int i = 0; i < brackets.size(); i++) {
            HrRules.TaxBracket b = brackets.get(i);
            boolean last = i == brackets.size() - 1;
            if (last != (b.upper() == null)) {
                throw new BizException("hr.error.taxTableLastUnlimited");
            }
            if (i > 0 && !last && b.upper().compareTo(brackets.get(i - 1).upper()) <= 0) {
                throw new BizException("hr.error.taxTableAscending", i + 1);
            }
        }
    }
}
