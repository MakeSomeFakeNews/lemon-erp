package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.system.dto.ImCallbackRequest;
import com.lemon.erp.modules.system.dto.ImCallbackResult;
import com.lemon.erp.modules.system.dto.ImProvider;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.service.SysImAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 钉钉、企业微信、飞书授权登录（免登录访问）
 */
@RestController
@RequestMapping("/auth/im")
@RequiredArgsConstructor
public class ImAuthController {

    private final SysImAuthService authService;

    /** 登录页显示的第三方登录方式（SaaS 模式按租户） */
    @GetMapping("/providers")
    public R<List<ImProvider>> providers(@RequestParam(required = false) String tenantName) {
        return R.ok(authService.providers(tenantName));
    }

    /**
     * 授权页地址
     *
     * @param tenantId 平台客户端内的免登入口带租户 ID；登录页使用租户名称
     * @param inApp    在平台客户端内打开
     * @param redirect 登录后进入的页面
     */
    @GetMapping("/{platform}/authorize")
    public R<String> authorize(@PathVariable ImPlatform platform,
                               @RequestParam(required = false) String tenantName,
                               @RequestParam(required = false) Long tenantId,
                               @RequestParam(defaultValue = "false") boolean inApp,
                               @RequestParam(required = false) String redirect) {
        return R.ok(authService.loginUrl(platform, tenantName, tenantId, inApp, redirect));
    }

    /** 授权回调：登录或绑定 */
    @PostMapping("/callback")
    public R<ImCallbackResult> callback(@Valid @RequestBody ImCallbackRequest req) {
        return R.ok(authService.callback(req));
    }
}
