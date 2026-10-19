package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.system.dto.MiniappBindRequest;
import com.lemon.erp.modules.system.dto.MiniappChooseRequest;
import com.lemon.erp.modules.system.dto.MiniappConfig;
import com.lemon.erp.modules.system.dto.MiniappLoginRequest;
import com.lemon.erp.modules.system.dto.MiniappLoginResult;
import com.lemon.erp.modules.system.miniapp.MiniappProperties;
import com.lemon.erp.modules.system.service.SysMiniappAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 微信小程序登录与账号绑定
 */
@RestController
@RequestMapping("/auth/miniapp")
@RequiredArgsConstructor
public class MiniappAuthController {

    private final SysMiniappAuthService authService;
    private final MiniappProperties properties;

    /** 小程序启动时读取：订阅消息模板（在用户点击时申请授权） */
    @GetMapping("/config")
    public R<MiniappConfig> config() {
        String template = properties.getSubscribe().getTemplateId();
        return R.ok(new MiniappConfig(StringUtils.hasText(template) ? List.of(template) : List.of()));
    }

    @PostMapping("/login")
    public R<MiniappLoginResult> login(@Valid @RequestBody MiniappLoginRequest req) {
        return R.ok(authService.login(req.code(), Boolean.TRUE.equals(req.switchAccount())));
    }

    @PostMapping("/choose")
    public R<MiniappLoginResult> choose(@Valid @RequestBody MiniappChooseRequest req) {
        return R.ok(authService.choose(req.key(), req.tenantId()));
    }

    @PostMapping("/bind")
    public R<MiniappLoginResult> bind(@Valid @RequestBody MiniappBindRequest req) {
        return R.ok(authService.bind(req));
    }

    /** 解除当前用户的微信绑定（需要登录） */
    @PostMapping("/unbind")
    public R<Void> unbind() {
        authService.unbindMine();
        return R.ok();
    }
}
