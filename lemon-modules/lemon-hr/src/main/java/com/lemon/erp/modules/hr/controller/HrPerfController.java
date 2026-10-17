package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.GradeStat;
import com.lemon.erp.modules.hr.dto.PerfPlanRequest;
import com.lemon.erp.modules.hr.dto.PerfScoreRequest;
import com.lemon.erp.modules.hr.dto.PerfStartRequest;
import com.lemon.erp.modules.hr.entity.HrPerfAppraisal;
import com.lemon.erp.modules.hr.entity.HrPerfPlan;
import com.lemon.erp.modules.hr.service.HrPerfService;
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
 * 绩效考核 /hr/perf：考核方案、考核表、评分
 */
@RestController
@RequestMapping("/hr/perf")
@RequiredArgsConstructor
public class HrPerfController {

    private final HrPerfService perfService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:perf:list')")
    public R<PageResult<HrPerfPlan>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status) {
        return R.ok(perfService.page(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:perf:list')")
    public R<HrPerfPlan> get(@PathVariable Long id) {
        return R.ok(perfService.detail(id));
    }

    @GetMapping("/{id}/grades")
    @PreAuthorize("@ss.hasPerm('hr:perf:list')")
    public R<List<GradeStat>> grades(@PathVariable Long id) {
        return R.ok(perfService.gradeStats(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('hr:perf:add')")
    @OperLog(title = "绩效考核", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody PerfPlanRequest req) {
        return R.ok(perfService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:perf:edit')")
    @OperLog(title = "绩效考核", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PerfPlanRequest req) {
        perfService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:perf:remove')")
    @OperLog(title = "绩效考核", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        perfService.delete(id);
        return R.ok();
    }

    /** 发起考核，返回考核人数 */
    @PostMapping("/{id}/start")
    @PreAuthorize("@ss.hasPerm('hr:perf:edit')")
    @OperLog(title = "绩效考核-发起", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Integer> start(@PathVariable Long id, @RequestBody(required = false) PerfStartRequest req) {
        return R.ok(perfService.start(id, req == null ? null : req.deptIds()));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("@ss.hasPerm('hr:perf:edit')")
    @OperLog(title = "绩效考核-归档", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> finish(@PathVariable Long id) {
        perfService.finish(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 考核表

    @GetMapping("/appraisal")
    @PreAuthorize("@ss.hasPerm('hr:perf:list')")
    public R<PageResult<HrPerfAppraisal>> appraisals(@RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) Long planId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Long deptId,
                                                     @RequestParam(required = false) String keyword) {
        return R.ok(perfService.appraisals(current, size, planId, status, deptId, null, null, keyword));
    }

    /** 登录即可访问：本人、评分人或有考核查看权限的用户可以查看 */
    @GetMapping("/appraisal/{id}")
    public R<HrPerfAppraisal> appraisal(@PathVariable Long id) {
        return R.ok(perfService.appraisal(id));
    }

    /** 登录即可访问：评分人评分，或有「代为评分」权限 */
    @PostMapping("/appraisal/{id}/review")
    @OperLog(title = "绩效考核-评分", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> review(@PathVariable Long id, @Valid @RequestBody PerfScoreRequest req) {
        perfService.review(id, req);
        return R.ok();
    }

    @PostMapping("/appraisal/{id}/reopen")
    @PreAuthorize("@ss.hasPerm('hr:perf:score')")
    @OperLog(title = "绩效考核-退回重评", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> reopen(@PathVariable Long id) {
        perfService.reopen(id);
        return R.ok();
    }
}
