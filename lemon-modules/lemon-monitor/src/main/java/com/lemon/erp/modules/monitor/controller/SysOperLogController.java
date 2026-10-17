package com.lemon.erp.modules.monitor.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.monitor.entity.SysOperLog;
import com.lemon.erp.modules.monitor.service.SysOperLogService;
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
@RequestMapping("/monitor/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService operLogService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('monitor:operlog:list')")
    public R<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String operator,
                                          @RequestParam(required = false) Integer operType,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginTime,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTime) {
        return R.ok(operLogService.page(current, size, title, operator, operType, status, beginTime, endTime));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('monitor:operlog:list')")
    public R<SysOperLog> detail(@PathVariable Long id) {
        return R.ok(operLogService.detail(id));
    }

    @DeleteMapping("/clean")
    @PreAuthorize("@ss.hasPerm('monitor:operlog:remove')")
    @OperLog(title = "操作日志", type = OperType.CLEAN) // i18n-ignore 操作日志存库
    public R<Void> clean() {
        operLogService.clean();
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('monitor:operlog:remove')")
    @OperLog(title = "操作日志", type = OperType.DELETE) // i18n-ignore 操作日志存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        operLogService.delete(ids);
        return R.ok();
    }
}
