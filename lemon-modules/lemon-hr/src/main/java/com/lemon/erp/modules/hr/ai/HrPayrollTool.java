package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.hr.dto.PayrollDeptRow;
import com.lemon.erp.modules.hr.dto.PayrollDeptSummary;
import com.lemon.erp.modules.hr.service.HrPayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 工资部门汇总：某月工资表按部门汇总的人数、应发、实发、扣款、单位社保公积金，复用 {@link HrPayrollService#deptSummary}。
 * 需要工资表的查看权限；<b>不提供个人薪资明细</b>：人数少于 {@link #MIN_GROUP} 的部门合并为一组（不足时再并入人数最少的部门），
 * 总人数不足时只给人数，避免从部门汇总推算出个人工资
 */
@Component
@RequiredArgsConstructor
public class HrPayrollTool implements AiTool {

    /** 单独列出金额的最少人数 */
    static final int MIN_GROUP = 3;

    /** 部门的工资汇总，金额单位元；第一行为合计 */
    record PayrollItem(String deptName, long headcount, BigDecimal grossPay, BigDecimal netPay, BigDecimal personalDeduct,
                       BigDecimal incomeTax, BigDecimal companyCost, BigDecimal avgGrossPay) {

        static PayrollItem of(String name, List<PayrollDeptRow> rows) {
            long n = rows.stream().mapToLong(r -> r.getHeadcount() == null ? 0 : r.getHeadcount()).sum();
            BigDecimal gross = Decimals.sum(rows, PayrollDeptRow::getGrossPay);
            return new PayrollItem(name, n, gross, Decimals.sum(rows, PayrollDeptRow::getNetPay), Decimals.sum(rows, PayrollDeptRow::getPersonalDeduct),
                    Decimals.sum(rows, PayrollDeptRow::getIncomeTax), Decimals.sum(rows, PayrollDeptRow::getCompanyCost),
                    n == 0 ? null : gross.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        }

        /** 人数不足时隐藏金额 */
        PayrollItem masked() {
            return new PayrollItem(deptName, headcount, null, null, null, null, null, null);
        }
    }

    private final HrPayrollService payrollService;

    @Override
    public String name() {
        return "hr_payroll_dept_summary";
    }

    @Override
    public String labelKey() {
        return "hr.ai.tool.payroll";
    }

    @Override
    public String descriptionKey() {
        return "hr.ai.tool.payroll.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return HrAiSupport.period(AiSchema.object(), "hr.ai.param.payrollMonth").build();
    }

    @Override
    public String permission() {
        return "hr:payroll:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        int period = HrAiSupport.period(args, YearMonth.now().minusMonths(1));
        PayrollDeptSummary summary = payrollService.deptSummary(period);
        List<PayrollDeptRow> small = new ArrayList<>();
        List<PayrollDeptRow> listed = new ArrayList<>();
        for (PayrollDeptRow r : summary.rows()) {
            (headcount(r) < MIN_GROUP ? small : listed).add(r);
        }
        // 合并后仍不足：再并入人数最少的部门，否则可以用「合计 - 其他部门」推算出这几个人的工资
        if (!small.isEmpty() && small.stream().mapToLong(HrPayrollTool::headcount).sum() < MIN_GROUP && !listed.isEmpty()) {
            PayrollDeptRow fewest = listed.stream().min(Comparator.comparingLong(HrPayrollTool::headcount)).orElseThrow();
            listed.remove(fewest);
            small.add(fewest);
        }
        List<PayrollItem> rows = new ArrayList<>();
        PayrollItem total = PayrollItem.of(HrAiSupport.totalLabel(), summary.rows());
        rows.add(total.headcount() < MIN_GROUP ? total.masked() : total);
        listed.forEach(r -> rows.add(PayrollItem.of(r.getDeptName(), List.of(r))));
        if (!small.isEmpty()) {
            PayrollItem merged = PayrollItem.of(Messages.get("hr.ai.text.smallDepts", MIN_GROUP), small);
            rows.add(merged.headcount() < MIN_GROUP ? merged.masked() : merged);
        }
        return AiToolResult.rows(rows);
    }

    private static long headcount(PayrollDeptRow r) {
        return r.getHeadcount() == null ? 0 : r.getHeadcount();
    }
}
