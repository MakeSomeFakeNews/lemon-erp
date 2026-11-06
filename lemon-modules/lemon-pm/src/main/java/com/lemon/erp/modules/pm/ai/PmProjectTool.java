package com.lemon.erp.modules.pm.ai;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.model.ProjectStatus;
import com.lemon.erp.modules.pm.service.PmProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 项目进度与成本：各项目的状态、计划日期、进度、任务完成与逾期、预算与实际成本（人工 + 费用）、合同额与回款，
 * 复用项目列表 {@link PmProjectService#page}（按数据权限：部门、项目经理）
 */
@Component
@RequiredArgsConstructor
public class PmProjectTool implements AiTool {

    /**
     * 项目，金额单位元
     *
     * @param progress          进度（%）
     * @param actualCost        实际成本 = 已审批工时的人工成本 + 已审批的费用
     * @param budgetUsedPercent 实际成本占预算（%）
     */
    record ProjectItem(String code, String name, ProjectStatus status, String managerName, String customerName, LocalDate planStart,
                       LocalDate planEnd, BigDecimal progress, Integer taskCount, Integer doneTaskCount, Integer overdueTaskCount,
                       BigDecimal budgetAmount, BigDecimal laborCost, BigDecimal expenseCost, BigDecimal actualCost, BigDecimal budgetUsedPercent,
                       BigDecimal contractAmount, BigDecimal billedAmount, BigDecimal receivedAmount) {
    }

    private final PmProjectService projectService;

    @Override
    public String name() {
        return "pm_project_overview";
    }

    @Override
    public String labelKey() {
        return "pm.ai.tool.projects";
    }

    @Override
    public String descriptionKey() {
        return "pm.ai.tool.projects.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("status", AiSchema.enumOf("pm.ai.param.projectStatus", ProjectStatus.PLANNING.name(), ProjectStatus.APPROVING.name(),
                        ProjectStatus.ACTIVE.name(), ProjectStatus.SUSPENDED.name(), ProjectStatus.FINISHED.name(), ProjectStatus.CLOSED.name()), false)
                .prop("keyword", AiSchema.string("pm.ai.param.projectKeyword").maxLength(64), false)
                .prop("top", AiSchema.integer("pm.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "pm:project:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        PageResult<PmProject> page = projectService.page(1, args.getInt("top", 50), args.getString("keyword"), args.getString("status"),
                null, false, null);
        return AiToolResult.rows(page.records().stream().map(p -> {
            BigDecimal actual = Decimals.nz(p.getLaborCost()).add(Decimals.nz(p.getExpenseCost()));
            return new ProjectItem(p.getCode(), p.getName(), p.getStatus(), p.getManagerName(), p.getCustomerName(), p.getPlanStart(),
                    p.getPlanEnd(), p.getProgress(), p.getTaskCount(), p.getDoneTaskCount(), p.getOverdueTaskCount(), p.getBudgetAmount(),
                    p.getLaborCost(), p.getExpenseCost(), actual, Decimals.percent(actual, p.getBudgetAmount(), 1), p.getContractAmount(),
                    p.getBilledAmount(), p.getReceivedAmount());
        }).toList(), page.total());
    }
}
