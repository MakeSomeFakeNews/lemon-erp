package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.JobRequest;
import com.lemon.erp.modules.system.entity.SysJob;
import com.lemon.erp.modules.system.entity.SysJobLog;
import com.lemon.erp.modules.system.service.SysJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 定时任务：查看、修改 Cron 与参数、启停、立即执行、执行日志
 */
@RestController
@RequestMapping("/monitor/job")
@RequiredArgsConstructor
public class SysJobController {

    private final SysJobService jobService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:job:list')")
    public R<List<SysJob>> list() {
        return R.ok(jobService.list());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('monitor:job:edit')")
    @OperLog(title = "定时任务", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody JobRequest req) {
        jobService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("@ss.hasPerm('monitor:job:run')")
    @OperLog(title = "定时任务-立即执行", type = OperType.OTHER)
    public R<SysJobLog> run(@PathVariable Long id) {
        return R.ok(jobService.runNow(id));
    }

    @GetMapping("/logs")
    @PreAuthorize("@ss.hasPerm('monitor:job:list')")
    public R<PageResult<SysJobLog>> logs(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String jobKey) {
        return R.ok(jobService.logs(current, size, jobKey));
    }
}
