package com.lemon.erp.modules.system.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.system.dto.AuthConfig;
import com.lemon.erp.modules.system.dto.CaptchaImage;
import com.lemon.erp.modules.system.dto.LoginRequest;
import com.lemon.erp.modules.system.dto.LoginResponse;
import com.lemon.erp.modules.system.dto.UserInfo;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.service.AuthService;
import com.lemon.erp.modules.system.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /** 前端的公开配置（不需要登录）：系统名称、登录验证码、密码策略等 */
    @GetMapping("/config")
    public R<AuthConfig> config() {
        return R.ok(authService.publicConfig());
    }

    /** 登录验证码图片（不需要登录） */
    @GetMapping("/captcha")
    public R<CaptchaImage> captcha() {
        return R.ok(captchaService.create());
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    /** 删除服务端会话，token 立即失效 */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R<UserInfo> info() {
        return R.ok(authService.currentUserInfo());
    }

    @GetMapping("/menus")
    public R<List<SysMenu>> menus() {
        return R.ok(authService.currentUserMenus());
    }
}
