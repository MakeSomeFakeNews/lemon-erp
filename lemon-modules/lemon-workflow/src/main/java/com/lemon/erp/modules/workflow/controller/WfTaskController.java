package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.workflow.dto.TaskCommentRequest;
import com.lemon.erp.modules.workflow.dto.TaskItem;
import com.lemon.erp.modules.workflow.dto.TaskTransferRequest;
import com.lemon.erp.modules.workflow.service.WfTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批任务：待办、已办、同意、拒绝、转交
 */
@RestController
@RequestMapping("/workflow/task")
@RequiredArgsConstructor
public class WfTaskController {

    private final WfTaskService taskService;

    @GetMapping("/todo")
    @PreAuthorize("@ss.hasPerm('wf:task:todo')")
    public R<PageResult<TaskItem>> todo(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String name) {
        return R.ok(taskService.pageTodo(name, current, size));
    }

    @GetMapping("/done")
    @PreAuthorize("@ss.hasPerm('wf:task:done')")
    public R<PageResult<TaskItem>> done(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String name) {
        return R.ok(taskService.pageDone(name, current, size));
    }

    // 会签等多人同时处理同一流程时，引擎以乐观锁检测冲突；控制器不在事务中，重试时审批整体在新事务中重做
    @PostMapping("/{taskId}/approve")
    @PreAuthorize("@ss.hasPerm('wf:task:todo')")
    @OperLog(title = "审批-同意", type = OperType.UPDATE)
    @Retryable(includes = FlowableOptimisticLockingException.class, maxRetries = 2, delay = 50)
    public R<Void> approve(@PathVariable String taskId, @Valid @RequestBody TaskCommentRequest req) {
        taskService.approve(taskId, req.comment());
        return R.ok();
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("@ss.hasPerm('wf:task:todo')")
    @OperLog(title = "审批-拒绝", type = OperType.UPDATE)
    @Retryable(includes = FlowableOptimisticLockingException.class, maxRetries = 2, delay = 50)
    public R<Void> reject(@PathVariable String taskId, @Valid @RequestBody TaskCommentRequest req) {
        taskService.reject(taskId, req.comment());
        return R.ok();
    }

    @PostMapping("/{taskId}/transfer")
    @PreAuthorize("@ss.hasPerm('wf:task:todo')")
    @OperLog(title = "审批-转交", type = OperType.UPDATE)
    public R<Void> transfer(@PathVariable String taskId, @Valid @RequestBody TaskTransferRequest req) {
        taskService.transfer(taskId, req.userId(), req.comment());
        return R.ok();
    }
}
