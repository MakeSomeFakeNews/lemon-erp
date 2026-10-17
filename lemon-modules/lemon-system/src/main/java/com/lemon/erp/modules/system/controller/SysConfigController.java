package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.system.dto.SettingItem;
import com.lemon.erp.modules.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 参数设置（系统管理，平台专属）
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('sys:config:list')")
    public R<List<SettingItem>> list() {
        return R.ok(configService.list());
    }

    /** 参数标识 → 值；值为空时恢复默认 */
    @PutMapping
    @PreAuthorize("@ss.hasPerm('sys:config:edit')")
    @OperLog(title = "参数设置", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> save(@RequestBody Map<String, String> values) {
        configService.save(values);
        return R.ok();
    }
}
