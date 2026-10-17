package com.lemon.erp.modules.pm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pm.dto.TaskRequest;
import com.lemon.erp.modules.pm.entity.PmTask;
import com.lemon.erp.modules.pm.service.PmTaskService;
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
 * 项目计划 /pm/task（任务树、甘特图），我的任务 /pm/my/tasks
 */
@RestController
@RequestMapping("/pm")
@RequiredArgsConstructor
public class PmTaskController {

    private final PmTaskService taskService;

    @GetMapping("/task")
    @PreAuthorize("@ss.hasAnyPerm('pm:task:list', 'pm:project:list', 'pm:timesheet:list', 'pm:accounting:list')")
    public R<List<PmTask>> tree(@RequestParam Long projectId) {
        return R.ok(taskService.tree(projectId));
    }

    @PostMapping("/task")
    @PreAuthorize("@ss.hasPerm('pm:task:edit')")
    @OperLog(title = "项目任务", type = OperType.INSERT)  // i18n-ignore 操作日志存库
    public R<Long> create(@Valid @RequestBody TaskRequest req) {
        return R.ok(taskService.create(req));
    }

    @PutMapping("/task/{id}")
    @PreAuthorize("@ss.hasPerm('pm:task:edit')")
    @OperLog(title = "项目任务", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        taskService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/task/{id}")
    @PreAuthorize("@ss.hasPerm('pm:task:edit')")
    @OperLog(title = "项目任务", type = OperType.DELETE)  // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    /** 登录即可访问：任务负责人更新进度（或有任务维护权限），body：{ progress } */
    @PutMapping("/task/{id}/progress")
    @OperLog(title = "项目任务-进度", type = OperType.UPDATE)  // i18n-ignore 操作日志存库
    public R<Void> progress(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        taskService.updateProgress(id, body.getOrDefault("progress", 0));
        return R.ok();
    }

    /** 登录即可访问：指派给我的未完成任务 */
    @GetMapping("/my/tasks")
    public R<List<PmTask>> myTasks() {
        return R.ok(taskService.myTasks());
    }
}
