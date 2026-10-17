package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.workflow.dto.InstanceDetail;
import com.lemon.erp.modules.workflow.dto.InstanceItem;
import com.lemon.erp.modules.workflow.dto.InstanceQuery;
import com.lemon.erp.modules.workflow.dto.ReasonRequest;
import com.lemon.erp.modules.workflow.dto.StartRequest;
import com.lemon.erp.modules.workflow.service.WfInstanceDetailService;
import com.lemon.erp.modules.workflow.service.WfInstanceService;
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

/**
 * 流程实例：发起、我的申请、流程管理
 */
@RestController
@RequestMapping("/workflow/instance")
@RequiredArgsConstructor
public class WfInstanceController {

    private final WfInstanceService instanceService;
    private final WfInstanceDetailService detailService;

    /** @return 流程实例 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('wf:start:list')")
    @OperLog(title = "发起流程", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<String> start(@Valid @RequestBody StartRequest req) {
        return R.ok(instanceService.startByForm(req.modelKey(), req.formData()));
    }

    @GetMapping("/mine")
    @PreAuthorize("@ss.hasPerm('wf:instance:mine')")
    public R<PageResult<InstanceItem>> mine(InstanceQuery query,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(instanceService.pageMine(query, current, size));
    }

    @GetMapping
    @PreAuthorize("@ss.hasPerm('wf:instance:list')")
    public R<PageResult<InstanceItem>> page(InstanceQuery query,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(instanceService.pageAll(query, current, size));
    }

    /** 发起人、审批人、抄送人或有流程管理权限的用户可以查看 */
    @GetMapping("/{id}")
    public R<InstanceDetail> detail(@PathVariable String id) {
        return R.ok(detailService.detail(id));
    }

    @PostMapping("/{id}/cancel")
    @OperLog(title = "撤回流程", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> cancel(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        instanceService.cancel(id, req.reason());
        return R.ok();
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("@ss.hasPerm('wf:instance:cancel')")
    @OperLog(title = "终止流程", type = OperType.FORCE)  // i18n-ignore 操作日志存库
    public R<Void> terminate(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        instanceService.terminate(id, req.reason());
        return R.ok();
    }
}
