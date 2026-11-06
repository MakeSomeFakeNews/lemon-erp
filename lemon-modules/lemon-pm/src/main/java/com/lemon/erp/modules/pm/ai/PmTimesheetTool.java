package com.lemon.erp.modules.pm.ai;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.pm.dto.TimesheetSummaryRow;
import com.lemon.erp.modules.pm.service.PmTimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工时汇总：日期范围内填报的工时按项目或成员汇总（已通过、待审批小时数），复用 {@link PmTimesheetService#summary}（按数据权限）。
 * 只给小时数，不给成员的小时成本（由薪资推算）
 */
@Component
@RequiredArgsConstructor
public class PmTimesheetTool implements AiTool {

    /** 汇总维度 */
    enum GroupBy {
        PROJECT, MEMBER
    }

    /** 项目或成员的工时：people 填报人数，projects 涉及项目数；第一行为合计（合计行只给成员数或项目数） */
    record TimesheetItem(String code, String name, BigDecimal approvedHours, BigDecimal pendingHours, Long people, Long projects) {
    }

    private final PmTimesheetService timesheetService;

    @Override
    public String name() {
        return "pm_timesheet_summary";
    }

    @Override
    public String labelKey() {
        return "pm.ai.tool.timesheets";
    }

    @Override
    public String descriptionKey() {
        return "pm.ai.tool.timesheets.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("groupBy", AiSchema.enumOf("pm.ai.param.timesheetGroupBy", GroupBy.PROJECT.name(), GroupBy.MEMBER.name()), false)
                .prop("startDate", AiSchema.date("pm.ai.param.startDate"), false)
                .prop("endDate", AiSchema.date("pm.ai.param.endDate"), false)
                .prop("top", AiSchema.integer("pm.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "pm:timesheet:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        boolean byUser = args.has("groupBy") && GroupBy.valueOf(args.getString("groupBy")) == GroupBy.MEMBER;
        LocalDate begin = args.has("startDate") ? args.getDate("startDate") : LocalDate.now().withDayOfMonth(1);
        LocalDate end = args.has("endDate") ? args.getDate("endDate") : LocalDate.now();
        List<TimesheetSummaryRow> list = timesheetService.summary(byUser, null, begin, end);
        List<TimesheetItem> rows = new ArrayList<>();
        rows.add(new TimesheetItem(null, Messages.get("pm.ai.text.total"),
                Decimals.sum(list, TimesheetSummaryRow::getApprovedHours), Decimals.sum(list, TimesheetSummaryRow::getPendingHours),
                byUser ? Long.valueOf(list.size()) : null, byUser ? null : Long.valueOf(list.size())));
        list.stream().limit(args.getInt("top", 50)).forEach(r -> rows.add(new TimesheetItem(r.getGroupCode(), r.getGroupName(),
                Decimals.nz(r.getApprovedHours()), Decimals.nz(r.getPendingHours()), r.getPeopleCount(), r.getProjectCount())));
        return AiToolResult.rows(rows, (long) list.size() + 1);
    }
}
