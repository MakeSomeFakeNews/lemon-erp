package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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

import java.util.List;
import com.lemon.erp.modules.fin.dto.BudgetExecution;
import com.lemon.erp.modules.fin.dto.BudgetRequest;
import com.lemon.erp.modules.fin.entity.FinBudget;
import com.lemon.erp.modules.fin.service.FinBudgetService;

/**
 * 预算编制与执行分析
 */
@RestController
@RequestMapping("/fin/budget")
@RequiredArgsConstructor
public class FinBudgetController {

    private final FinBudgetService budgetService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('fin:budget:list')")
    public R<List<FinBudget>> list(@RequestParam int year, @RequestParam(required = false) Long deptId) {
        return R.ok(budgetService.list(year, deptId));
    }

    @GetMapping("/execution")
    @PreAuthorize("@ss.hasPerm('fin:budget:list')")
    public R<List<BudgetExecution>> execution(@RequestParam int year, @RequestParam(required = false) Long deptId) {
        return R.ok(budgetService.execution(year, deptId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('fin:budget:edit')")
    @OperLog(title = "预算", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody BudgetRequest req) {
        return R.ok(budgetService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('fin:budget:edit')")
    @OperLog(title = "预算", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BudgetRequest req) {
        budgetService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:budget:edit')")
    @OperLog(title = "预算", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        budgetService.delete(ids);
        return R.ok();
    }
}
