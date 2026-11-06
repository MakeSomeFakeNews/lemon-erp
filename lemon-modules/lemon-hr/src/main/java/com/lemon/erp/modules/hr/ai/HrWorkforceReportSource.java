package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.framework.ai.AiReportMetrics;
import com.lemon.erp.framework.ai.AiReportPeriod;
import com.lemon.erp.framework.ai.AiReportSource;
import com.lemon.erp.modules.hr.dto.EmployeeStats;
import com.lemon.erp.modules.hr.service.HrEmployeeService;
import com.lemon.erp.modules.hr.service.HrWorkforceSummaryService;
import com.lemon.erp.modules.hr.service.HrWorkforceSummaryService.PeriodSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能报告「人事」：本期与上期的期初 / 期末在职人数、入职、离职、离职率，当前试用期人数和按部门的在职人数
 * （员工概况 {@link HrEmployeeService#stats()}）。只有人数，不含薪资等个人信息；汇总全公司员工，只对有「全部数据」权限的用户开放
 */
@Component
@RequiredArgsConstructor
public class HrWorkforceReportSource implements AiReportSource {

    private static final int TOP_DEPTS = 10;

    private final HrWorkforceSummaryService summaryService;
    private final HrEmployeeService employeeService;

    @Override
    public String key() {
        return "hr.workforce";
    }

    @Override
    public String labelKey() {
        return "hr.ai.report.workforce";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public String permission() {
        return "hr:employee:list";
    }

    @Override
    public boolean requiresAllData() {
        return true;
    }

    @Override
    public WorkforceSnapshot snapshot(AiReportPeriod period) {
        PeriodSummary current = summaryService.summary(period.start(), period.end());
        AiReportPeriod prev = period.previous();
        PeriodSummary previous = summaryService.summary(prev.start(), prev.end());
        EmployeeStats stats = employeeService.stats();
        List<DeptCount> byDept = stats.byDept().stream().limit(TOP_DEPTS)
                .map(HrWorkforceReportSource::dept).toList();
        return new WorkforceSnapshot(current, previous, AiReportMetrics.growth(current.headcountEnd(), previous.headcountEnd()),
                current.turnoverRatePct() == null || previous.turnoverRatePct() == null ? null
                        : current.turnoverRatePct().subtract(previous.turnoverRatePct()),
                stats.probation(), byDept);
    }

    private static DeptCount dept(Map<String, Object> m) {
        Object name = m.get("name");
        Object value = m.get("value");
        return new DeptCount(name == null ? "" : name.toString(), value == null ? 0 : ((Number) value).longValue());
    }

    /**
     * 人事快照
     *
     * @param headcountChangePct     期末在职人数比上期末的变化（%）
     * @param turnoverChangePoints   离职率比上期变化（百分点）
     * @param probationNow           当前试用期人数
     * @param headcountByDeptNow     当前各部门在职人数（前 10）
     */
    public record WorkforceSnapshot(PeriodSummary current, PeriodSummary previous, BigDecimal headcountChangePct,
                                    BigDecimal turnoverChangePoints, long probationNow, List<DeptCount> headcountByDeptNow) {
    }

    public record DeptCount(String dept, long headcount) {
    }
}
