package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.ApproveRequest;
import com.lemon.erp.modules.hr.dto.LeaveQuery;
import com.lemon.erp.modules.hr.entity.HrLeave;
import com.lemon.erp.modules.hr.service.HrLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 假勤审批 /hr/leave（员工提交、撤销在员工自助 /hr/self）
 */
@RestController
@RequestMapping("/hr/leave")
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveService leaveService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:leave:list')")
    public R<PageResult<HrLeave>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String kind,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long deptId,
                                       @RequestParam(required = false) LocalDate beginDate,
                                       @RequestParam(required = false) LocalDate endDate) {
        return R.ok(leaveService.page(current, size, new LeaveQuery(keyword, kind, type, status, deptId, beginDate,
                endDate == null ? null : endDate.plusDays(1))));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@ss.hasPerm('hr:leave:approve')")
    @OperLog(title = "假勤审批", type = OperType.UPDATE)
    public R<Void> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest req) {
        leaveService.approve(id, req);
        return R.ok();
    }
}
