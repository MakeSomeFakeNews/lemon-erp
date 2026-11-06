package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ai.dto.AiProviderOption;
import com.lemon.erp.modules.ai.dto.AiStatus;
import com.lemon.erp.modules.ai.dto.AiTestResult;
import com.lemon.erp.modules.ai.dto.AiUsageSummary;
import com.lemon.erp.modules.ai.entity.AiConfig;
import com.lemon.erp.modules.ai.service.AiConfigService;
import com.lemon.erp.modules.ai.service.AiUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 「AI 设置」：大模型服务商、API Key（只写不读）、模型、功能开关、月度上限，测试连接，本月用量
 */
@RestController
@RequestMapping("/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService configService;
    private final AiUsageService usageService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public R<AiConfig> get() {
        return R.ok(configService.get());
    }

    @GetMapping("/providers")
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public R<List<AiProviderOption>> providers() {
        return R.ok(configService.providers());
    }

    /** API Key 为空表示不修改 */
    @PutMapping
    @PreAuthorize("@ss.hasPerm('ai:config:edit')")
    @OperLog(title = "AI 设置", type = OperType.UPDATE) // i18n-ignore 操作日志存库
    public R<Void> save(@Valid @RequestBody AiConfig config) {
        configService.save(config);
        return R.ok();
    }

    /** 用页面上填写的配置测试（API Key 为空时用已保存的），返回模型的回复或错误原因 */
    @PostMapping("/test")
    @PreAuthorize("@ss.hasPerm('ai:config:edit')")
    public R<AiTestResult> test(@Valid @RequestBody AiConfig config) {
        return R.ok(configService.test(config));
    }

    @GetMapping("/usage")
    @PreAuthorize("@ss.hasPerm('ai:config:list')")
    public R<AiUsageSummary> usage() {
        return R.ok(usageService.summary(configService.find()));
    }

    /** 当前租户各 AI 功能是否可用（登录即可，业务页面据此显示引导，不含任何配置内容） */
    @GetMapping("/status")
    public R<AiStatus> status() {
        return R.ok(configService.status());
    }
}
