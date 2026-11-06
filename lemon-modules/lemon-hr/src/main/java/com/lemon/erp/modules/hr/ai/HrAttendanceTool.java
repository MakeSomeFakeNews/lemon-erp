package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.service.HrAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤异常汇总：某月迟到、早退、缺卡次数和旷工天数，按部门汇总或列出有异常的员工，
 * 复用考勤月度汇总 {@link HrAttendanceService#summaryPage}（按数据权限）
 */
@Component
@RequiredArgsConstructor
public class HrAttendanceTool implements AiTool {

    /** 汇总维度 */
    enum GroupBy {
        DEPT, EMPLOYEE
    }

    /**
     * 部门或员工的考勤异常；第一行为合计
     *
     * @param employees 人数（按员工列出时为空）
     * @param leaveHours 已通过的请假小时
     */
    record AttendanceItem(String name, String deptName, Integer employees, int shouldDays, int actualDays, int lateCount, int earlyCount,
                          int missingCount, int absentDays, BigDecimal leaveHours, BigDecimal overtimeHours) {

        int exceptions() {
            return lateCount + earlyCount + missingCount + absentDays;
        }

        AttendanceItem plus(AttendanceSummary s) {
            return new AttendanceItem(name, deptName, employees == null ? null : employees + 1, shouldDays + s.shouldDays(),
                    actualDays + s.actualDays(), lateCount + s.lateCount(), earlyCount + s.earlyCount(), missingCount + s.missingCount(),
                    absentDays + s.absentDays(), leaveHours.add(Decimals.sum(s.leaveHours().values())), overtimeHours.add(Decimals.nz(s.overtimeHours())));
        }

        static AttendanceItem empty(String name, String deptName, Integer employees) {
            return new AttendanceItem(name, deptName, employees, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private final HrAttendanceService attendanceService;

    @Override
    public String name() {
        return "hr_attendance_exceptions";
    }

    @Override
    public String labelKey() {
        return "hr.ai.tool.attendance";
    }

    @Override
    public String descriptionKey() {
        return "hr.ai.tool.attendance.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = HrAiSupport.period(AiSchema.object(), "hr.ai.param.month")
                .prop(HrAiSupport.GROUP_BY, AiSchema.enumOf("hr.ai.param.attendanceGroupBy", GroupBy.DEPT.name(), GroupBy.EMPLOYEE.name()), false)
                .prop("keyword", AiSchema.string("hr.ai.param.employeeKeyword").maxLength(64), false);
        return s.build();
    }

    @Override
    public String permission() {
        return "hr:attendance:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        int period = HrAiSupport.period(args, YearMonth.now());
        GroupBy group = HrAiSupport.enumArg(args, HrAiSupport.GROUP_BY, GroupBy.class, GroupBy.DEPT);
        List<AttendanceSummary> list = attendanceService.summaryPage(period, null, args.getString("keyword"));
        AttendanceItem total = AttendanceItem.empty(HrAiSupport.totalLabel(), null, 0);
        Map<String, AttendanceItem> byDept = new LinkedHashMap<>();
        List<AttendanceItem> employees = new ArrayList<>();
        for (AttendanceSummary s : list) {
            total = total.plus(s);
            if (group == GroupBy.DEPT) {
                byDept.merge(String.valueOf(s.deptName()), AttendanceItem.empty(s.deptName(), null, 0).plus(s),
                        (a, b) -> a.plus(s));
            } else {
                AttendanceItem item = AttendanceItem.empty(s.employeeName(), s.deptName(), null).plus(s);
                if (item.exceptions() > 0) {
                    employees.add(item);
                }
            }
        }
        List<AttendanceItem> rows = new ArrayList<>();
        rows.add(total);
        List<AttendanceItem> detail = group == GroupBy.DEPT ? new ArrayList<>(byDept.values()) : employees;
        detail.sort(Comparator.comparingInt(AttendanceItem::absentDays).thenComparingInt(AttendanceItem::exceptions).reversed());
        rows.addAll(detail);
        return AiToolResult.rows(rows);
    }
}
