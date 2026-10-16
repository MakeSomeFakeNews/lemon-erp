package com.lemon.erp.modules.pm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pm.dto.ReviewRequest;
import com.lemon.erp.modules.pm.dto.TimesheetRequest;
import com.lemon.erp.modules.pm.dto.TimesheetSaved;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import com.lemon.erp.modules.pm.service.PmTimesheetService;
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
 * 工时审批 /pm/timesheet，本人填报 /pm/my/timesheet（登录即可访问，须为项目成员）
 */
@RestController
@RequestMapping("/pm")
@RequiredArgsConstructor
public class PmTimesheetController {

    private final PmTimesheetService timesheetService;

    @GetMapping("/timesheet")
    @PreAuthorize("@ss.hasPerm('pm:timesheet:list')")
    public R<PageResult<PmTimesheet>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long projectId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) LocalDate beginDate,
                                           @RequestParam(required = false) LocalDate endDate) {
        return R.ok(timesheetService.page(current, size, projectId, userId, status, beginDate, endDate));
    }

    /** 审批通过 / 驳回，返回处理条数 */
    @PostMapping("/timesheet/review")
    @PreAuthorize("@ss.hasPerm('pm:timesheet:approve')")
    @OperLog(title = "工时审批", type = OperType.UPDATE)
    public R<Integer> review(@Valid @RequestBody ReviewRequest req) {
        return R.ok(timesheetService.review(req));
    }

    @GetMapping("/my/timesheet")
    public R<List<PmTimesheet>> mine(@RequestParam LocalDate beginDate, @RequestParam LocalDate endDate) {
        return R.ok(timesheetService.mine(beginDate, endDate));
    }

    @PostMapping("/my/timesheet")
    @OperLog(title = "填报工时", type = OperType.INSERT)
    public R<TimesheetSaved> create(@Valid @RequestBody TimesheetRequest req) {
        return R.ok(timesheetService.save(null, req));
    }

    @PutMapping("/my/timesheet/{id}")
    @OperLog(title = "填报工时", type = OperType.UPDATE)
    public R<TimesheetSaved> update(@PathVariable Long id, @Valid @RequestBody TimesheetRequest req) {
        return R.ok(timesheetService.save(id, req));
    }

    @DeleteMapping("/my/timesheet/{id}")
    @OperLog(title = "填报工时", type = OperType.DELETE)
    public R<Void> delete(@PathVariable Long id) {
        timesheetService.delete(id);
        return R.ok();
    }
}
