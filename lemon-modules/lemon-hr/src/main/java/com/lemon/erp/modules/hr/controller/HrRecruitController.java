package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.CandidateRequest;
import com.lemon.erp.modules.hr.dto.HireRequest;
import com.lemon.erp.modules.hr.dto.InterviewRequest;
import com.lemon.erp.modules.hr.dto.RecruitRequest;
import com.lemon.erp.modules.hr.entity.HrCandidate;
import com.lemon.erp.modules.hr.entity.HrInterview;
import com.lemon.erp.modules.hr.entity.HrRecruit;
import com.lemon.erp.modules.hr.service.HrRecruitService;
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
import java.util.Map;

/**
 * 招聘需求 /hr/recruit、应聘者 /hr/candidate、面试 /hr/interview
 */
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrRecruitController {

    private final HrRecruitService recruitService;

    @GetMapping("/recruit")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<PageResult<HrRecruit>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) Long deptId) {
        return R.ok(recruitService.page(current, size, keyword, status, deptId));
    }

    @GetMapping("/recruit/open")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<List<HrRecruit>> openOptions() {
        return R.ok(recruitService.openOptions());
    }

    @GetMapping("/recruit/{id}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<HrRecruit> get(@PathVariable Long id) {
        return R.ok(recruitService.detail(id));
    }

    @PostMapping("/recruit")
    @PreAuthorize("@ss.hasPerm('hr:recruit:add')")
    @OperLog(title = "招聘需求", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody RecruitRequest req) {
        return R.ok(recruitService.create(req));
    }

    @PutMapping("/recruit/{id}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:edit')")
    @OperLog(title = "招聘需求", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RecruitRequest req) {
        recruitService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/recruit/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:remove')")
    @OperLog(title = "招聘需求", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        recruitService.delete(ids);
        return R.ok();
    }

    /** 审核：返回 true 表示已发起审批流程 */
    @PostMapping("/recruit/{id}/approve")
    @PreAuthorize("@ss.hasPerm('hr:recruit:approve')")
    @OperLog(title = "招聘需求-审核", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Boolean> approve(@PathVariable Long id) {
        return R.ok(recruitService.approve(id));
    }

    @PostMapping("/recruit/{id}/withdraw")
    @PreAuthorize("@ss.hasPerm('hr:recruit:approve')")
    @OperLog(title = "招聘需求-撤回", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> withdraw(@PathVariable Long id) {
        recruitService.withdraw(id);
        return R.ok();
    }

    @PostMapping("/recruit/{id}/close")
    @PreAuthorize("@ss.hasPerm('hr:recruit:edit')")
    @OperLog(title = "招聘需求-关闭", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> close(@PathVariable Long id) {
        recruitService.close(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 应聘者

    @GetMapping("/candidate")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<PageResult<HrCandidate>> candidates(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Long recruitId,
                                                 @RequestParam(required = false) String stage,
                                                 @RequestParam(required = false) String keyword) {
        return R.ok(recruitService.candidatePage(current, size, recruitId, stage, keyword));
    }

    @GetMapping("/candidate/{id}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<HrCandidate> candidate(@PathVariable Long id) {
        return R.ok(recruitService.candidate(id));
    }

    @PostMapping("/candidate")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> createCandidate(@Valid @RequestBody CandidateRequest req) {
        return R.ok(recruitService.createCandidate(req));
    }

    @PutMapping("/candidate/{id}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> updateCandidate(@PathVariable Long id, @Valid @RequestBody CandidateRequest req) {
        recruitService.updateCandidate(id, req);
        return R.ok();
    }

    @DeleteMapping("/candidate/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> deleteCandidates(@PathVariable List<Long> ids) {
        recruitService.deleteCandidates(ids);
        return R.ok();
    }

    @GetMapping("/candidate/{id}/interviews")
    @PreAuthorize("@ss.hasPerm('hr:recruit:list')")
    public R<List<HrInterview>> interviews(@PathVariable Long id) {
        return R.ok(recruitService.interviews(id));
    }

    @PostMapping("/candidate/{id}/interview")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者-安排面试", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> addInterview(@PathVariable Long id, @Valid @RequestBody InterviewRequest req) {
        return R.ok(recruitService.addInterview(id, req));
    }

    @PutMapping("/interview/{id}")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者-面试结果", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> updateInterview(@PathVariable Long id, @Valid @RequestBody InterviewRequest req) {
        recruitService.updateInterview(id, req);
        return R.ok();
    }

    @PostMapping("/candidate/{id}/offer")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者-发Offer", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> offer(@PathVariable Long id) {
        recruitService.offer(id);
        return R.ok();
    }

    /** 淘汰，body：{ reason } */
    @PostMapping("/candidate/{id}/reject")
    @PreAuthorize("@ss.hasPerm('hr:recruit:candidate')")
    @OperLog(title = "应聘者-淘汰", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        recruitService.reject(id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    /** 录用入职，返回新员工 ID */
    @PostMapping("/candidate/{id}/hire")
    @PreAuthorize("@ss.hasPerm('hr:recruit:hire')")
    @OperLog(title = "应聘者-录用入职", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> hire(@PathVariable Long id, @Valid @RequestBody HireRequest req) {
        return R.ok(recruitService.hire(id, req));
    }
}
