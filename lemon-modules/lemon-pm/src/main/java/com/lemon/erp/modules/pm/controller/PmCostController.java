package com.lemon.erp.modules.pm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pm.dto.BudgetRequest;
import com.lemon.erp.modules.pm.dto.CostRequest;
import com.lemon.erp.modules.pm.dto.IssueRequest;
import com.lemon.erp.modules.pm.entity.PmBudget;
import com.lemon.erp.modules.pm.entity.PmCost;
import com.lemon.erp.modules.pm.service.PmCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 成本控制：项目费用 /pm/cost、预算 /pm/project/{id}/budget、项目领料 /pm/project/{id}/issue
 */
@RestController
@RequestMapping("/pm")
@RequiredArgsConstructor
public class PmCostController {

    private final PmCostService costService;

    @GetMapping("/cost")
    @PreAuthorize("@ss.hasAnyPerm('pm:cost:list', 'pm:accounting:list')")
    public R<PageResult<PmCost>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Long projectId,
                                      @RequestParam(required = false) String costType,
                                      @RequestParam(required = false) LocalDate beginDate,
                                      @RequestParam(required = false) LocalDate endDate) {
        return R.ok(costService.page(current, size, projectId, costType, beginDate, endDate));
    }

    @PostMapping("/cost")
    @PreAuthorize("@ss.hasPerm('pm:cost:add')")
    @OperLog(title = "项目费用", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody CostRequest req) {
        return R.ok(costService.create(req));
    }

    @DeleteMapping("/cost/{ids}")
    @PreAuthorize("@ss.hasPerm('pm:cost:remove')")
    @OperLog(title = "项目费用", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        costService.delete(ids);
        return R.ok();
    }

    /** 各成本类型的预算与实际 */
    @GetMapping("/project/{id}/budget")
    @PreAuthorize("@ss.hasAnyPerm('pm:cost:list', 'pm:accounting:list')")
    public R<List<PmBudget>> budgets(@PathVariable Long id) {
        return R.ok(costService.budgets(id));
    }

    @PutMapping("/project/{id}/budget")
    @PreAuthorize("@ss.hasPerm('pm:cost:budget')")
    @OperLog(title = "项目预算", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> saveBudgets(@PathVariable Long id, @Valid @RequestBody BudgetRequest req) {
        costService.saveBudgets(id, req);
        return R.ok();
    }

    @PostMapping("/project/{id}/issue")
    @PreAuthorize("@ss.hasPerm('pm:cost:issue')")
    @OperLog(title = "项目领料", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> issue(@PathVariable Long id, @Valid @RequestBody IssueRequest req) {
        return R.ok(costService.issue(id, req));
    }
}
