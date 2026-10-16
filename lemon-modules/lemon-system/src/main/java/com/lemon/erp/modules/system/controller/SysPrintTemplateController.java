package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.service.SysPrintTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 打印模板（系统管理）
 */
@RestController
@RequestMapping("/system/print-template")
@RequiredArgsConstructor
public class SysPrintTemplateController {

    private final SysPrintTemplateService templateService;

    /** 已修改的模板，所有登录用户打印单据时使用 */
    @GetMapping
    public R<Map<String, Object>> all() {
        return R.ok(templateService.all());
    }

    @PutMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板", type = OperType.UPDATE)
    public R<Void> save(@PathVariable String key, @RequestBody Map<String, Object> config) {
        templateService.save(key, config);
        return R.ok();
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("@ss.hasPerm('sys:printTemplate:edit')")
    @OperLog(title = "打印模板-恢复默认", type = OperType.UPDATE)
    public R<Void> reset(@PathVariable String key) {
        templateService.reset(key);
        return R.ok();
    }
}
