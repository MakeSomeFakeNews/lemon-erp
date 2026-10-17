package com.lemon.erp.modules.monitor.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.monitor.entity.SysLoginLog;
import com.lemon.erp.modules.monitor.service.SysLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/monitor/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:loginlog:list')")
    public R<PageResult<SysLoginLog>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String ip,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginTime,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTime) {
        return R.ok(loginLogService.page(current, size, username, ip, location, status, beginTime, endTime));
    }

    @DeleteMapping("/clean")
    @PreAuthorize("@ss.hasPerm('monitor:loginlog:remove')")
    @OperLog(title = "登录日志", type = OperType.CLEAN) // i18n-ignore 操作日志存库
    public R<Void> clean() {
        loginLogService.clean();
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('monitor:loginlog:remove')")
    @OperLog(title = "登录日志", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        loginLogService.delete(ids);
        return R.ok();
    }
}
