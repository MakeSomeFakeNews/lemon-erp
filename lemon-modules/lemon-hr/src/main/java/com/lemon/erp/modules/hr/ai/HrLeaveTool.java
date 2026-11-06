package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.hr.dto.LeaveSummaryRow;
import com.lemon.erp.modules.hr.model.LeaveKind;
import com.lemon.erp.modules.hr.service.HrLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 请假 / 加班汇总：日期范围内已通过的申请按假期类型（加班为补偿方式）或部门汇总，复用 {@link HrLeaveService#summary}（按数据权限）
 */
@Component
@RequiredArgsConstructor
public class HrLeaveTool implements AiTool {

    /** 汇总维度 */
    enum GroupBy {
        TYPE, DEPT
    }

    /** 一组的申请数、人数、小时数；第一行为合计（人数为各组之和，同一人可能计入多组） */
    record LeaveItem(String name, long applyCount, long employeeCount, BigDecimal hours) {
    }

    private final HrLeaveService leaveService;

    @Override
    public String name() {
        return "hr_leave_summary";
    }

    @Override
    public String labelKey() {
        return "hr.ai.tool.leave";
    }

    @Override
    public String descriptionKey() {
        return "hr.ai.tool.leave.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("kind", AiSchema.enumOf("hr.ai.param.leaveKind", LeaveKind.LEAVE.name(), LeaveKind.OVERTIME.name()), false)
                .prop(HrAiSupport.GROUP_BY, AiSchema.enumOf("hr.ai.param.leaveGroupBy", GroupBy.TYPE.name(), GroupBy.DEPT.name()), false);
        return HrAiSupport.dateRange(s).build();
    }

    @Override
    public String permission() {
        return "hr:leave:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        LeaveKind kind = HrAiSupport.enumArg(args, "kind", LeaveKind.class, LeaveKind.LEAVE);
        boolean byDept = HrAiSupport.enumArg(args, HrAiSupport.GROUP_BY, GroupBy.class, GroupBy.TYPE) == GroupBy.DEPT;
        List<LeaveSummaryRow> list = leaveService.summary(kind, byDept, HrAiSupport.start(args), HrAiSupport.end(args));
        List<LeaveItem> rows = new ArrayList<>();
        rows.add(new LeaveItem(HrAiSupport.totalLabel(), list.stream().mapToLong(r -> n(r.getApplyCount())).sum(),
                list.stream().mapToLong(r -> n(r.getEmployeeCount())).sum(), Decimals.sum(list, LeaveSummaryRow::getHours)));
        list.forEach(r -> rows.add(new LeaveItem(r.getGroupName(), n(r.getApplyCount()), n(r.getEmployeeCount()), Decimals.nz(r.getHours()))));
        return AiToolResult.rows(rows);
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
