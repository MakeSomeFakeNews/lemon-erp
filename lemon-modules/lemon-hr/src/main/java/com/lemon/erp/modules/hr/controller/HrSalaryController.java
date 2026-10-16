package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.SalaryRequest;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.entity.HrSalaryHistory;
import com.lemon.erp.modules.hr.service.HrSalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资档案 /hr/salary，按员工维护
 */
import java.util.List;

@RestController
@RequestMapping("/hr/salary")
@RequiredArgsConstructor
public class HrSalaryController {

    private final HrSalaryService salaryService;

    /** @param missing true 只看没有薪资档案的在职员工 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:salary:list')")
    public R<PageResult<HrSalary>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long deptId,
                                        @RequestParam(required = false) Boolean missing) {
        return R.ok(salaryService.page(current, size, keyword, deptId, missing));
    }

    /** 薪资历史（按生效日期倒序） */
    @GetMapping("/{employeeId}/history")
    @PreAuthorize("@ss.hasPerm('hr:salary:list')")
    public R<List<HrSalaryHistory>> history(@PathVariable Long employeeId) {
        return R.ok(salaryService.history(employeeId));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("@ss.hasPerm('hr:salary:list')")
    public R<HrSalary> get(@PathVariable Long employeeId) {
        return R.ok(salaryService.ofEmployee(employeeId));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("@ss.hasPerm('hr:salary:edit')")
    @OperLog(title = "薪资档案", type = OperType.UPDATE)
    public R<Void> save(@PathVariable Long employeeId, @Valid @RequestBody SalaryRequest req) {
        salaryService.save(employeeId, req);
        return R.ok();
    }
}
