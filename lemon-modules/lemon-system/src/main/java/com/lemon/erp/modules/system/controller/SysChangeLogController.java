package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.system.entity.SysChangeLog;
import com.lemon.erp.modules.system.service.SysChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字段变更历史：权限按业务类型和业务记录在服务中校验
 */
@RestController
@RequestMapping("/system/change-log")
@RequiredArgsConstructor
public class SysChangeLogController {

    private final SysChangeLogService changeLogService;

    @GetMapping
    public R<PageResult<SysChangeLog>> page(@RequestParam String bizType, @RequestParam Long bizId,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size) {
        return R.ok(changeLogService.page(bizType, bizId, current, size));
    }
}
