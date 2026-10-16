package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.EmployeeChangeRequest;
import com.lemon.erp.modules.hr.dto.EmployeeOption;
import com.lemon.erp.modules.hr.dto.EmployeeRequest;
import com.lemon.erp.modules.hr.dto.EmployeeStats;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrEmployeeChange;
import com.lemon.erp.modules.hr.entity.HrTrainingRecord;
import com.lemon.erp.modules.hr.service.HrEmployeeService;
import com.lemon.erp.modules.hr.service.HrTrainingService;
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

/**
 * 员工档案 /hr/employee
 */
@RestController
@RequestMapping("/hr/employee")
@RequiredArgsConstructor
public class HrEmployeeController {

    private final HrEmployeeService employeeService;
    private final HrTrainingService trainingService;
    private final ImportExecutor importExecutor;

    /** @param deptId 部门（含下级），status 为空时不含离职员工 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:employee:list')")
    public R<PageResult<HrEmployee>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long deptId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long positionId,
                                          @RequestParam(required = false) String empType) {
        return R.ok(employeeService.page(current, size, keyword, deptId, status, positionId, empType));
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPerm('hr:employee:list')")
    public R<EmployeeStats> stats() {
        return R.ok(employeeService.stats());
    }

    /** 登录即可访问：培训学员、考勤补录、工时等选择员工 */
    @GetMapping("/options")
    public R<List<EmployeeOption>> options(@RequestParam(required = false) String keyword) {
        return R.ok(employeeService.options(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:employee:list')")
    public R<HrEmployee> get(@PathVariable Long id) {
        return R.ok(employeeService.detail(id));
    }

    @GetMapping("/{id}/changes")
    @PreAuthorize("@ss.hasPerm('hr:employee:list')")
    public R<List<HrEmployeeChange>> changes(@PathVariable Long id) {
        return R.ok(employeeService.changes(id));
    }

    @GetMapping("/{id}/trainings")
    @PreAuthorize("@ss.hasPerm('hr:employee:list')")
    public R<List<HrTrainingRecord>> trainings(@PathVariable Long id) {
        return R.ok(trainingService.employeeRecords(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('hr:employee:add')")
    @OperLog(title = "员工档案", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EmployeeRequest req) {
        return R.ok(employeeService.create(req));
    }


    /** Excel 导入：逐行按新增校验，任何一行出错整批不导入 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('hr:employee:add')")
    @OperLog(title = "员工档案-导入", type = OperType.IMPORT)
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<EmployeeRequest> req) {
        return R.ok(importExecutor.run(req, employeeService::create));
    }
    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:employee:edit')")
    @OperLog(title = "员工档案", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest req) {
        employeeService.update(id, req);
        return R.ok();
    }

    /** 人事异动：转正、调岗、调薪、离职。返回 true 表示已提交审批 */
    @PostMapping("/{id}/change")
    @PreAuthorize("@ss.hasPerm('hr:employee:change')")
    @OperLog(title = "人事异动", type = OperType.UPDATE)
    public R<Boolean> change(@PathVariable Long id, @Valid @RequestBody EmployeeChangeRequest req) {
        return R.ok(employeeService.change(id, req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:employee:remove')")
    @OperLog(title = "员工档案", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        employeeService.delete(ids);
        return R.ok();
    }
}
