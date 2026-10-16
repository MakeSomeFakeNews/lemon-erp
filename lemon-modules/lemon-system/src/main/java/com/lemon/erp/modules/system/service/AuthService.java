package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.ServletUtils;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.common.util.UserAgentUtils;
import com.lemon.erp.framework.demo.DemoProperties;
import com.lemon.erp.framework.portal.PortalKind;
import com.lemon.erp.framework.log.LoginLogEvent;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.framework.setting.CaptchaMode;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.framework.tenant.PlatformFeatures;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.dto.AuthConfig;
import com.lemon.erp.modules.system.dto.LoginRequest;
import com.lemon.erp.modules.system.dto.LoginResponse;
import com.lemon.erp.modules.system.dto.UserInfo;
import com.lemon.erp.modules.system.entity.SysMenu;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysMenuMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher publisher;
    private final PasswordPolicy passwordPolicy;
    private final SysTenantLookup tenantLookup;
    private final SettingService settings;
    private final CaptchaService captchaService;
    private final DemoProperties demoProperties;

    public AuthConfig publicConfig() {
        return new AuthConfig(tenantLookup.isEnabled(), settings.get(Setting.SYSTEM_NAME), settings.getBool(Setting.SHOW_DEMO_ACCOUNT),
                settings.getBool(Setting.WATERMARK), settings.get(Setting.CAPTCHA_MODE), settings.getInt(Setting.PASSWORD_MIN_LENGTH),
                settings.get(Setting.PASSWORD_COMPLEXITY), settings.getInt(Setting.ATTACHMENT_MAX_MB), demoProperties.isEnabled());
    }

    public LoginResponse login(LoginRequest req) {
        HttpServletRequest request = ServletUtils.getRequest();
        // 先确定租户，之后的查询（用户、登录日志）都在该租户下
        TenantContext.set(TenantContext.PLATFORM_TENANT_ID);
        try {
            TenantContext.set(tenantLookup.resolveLoginTenant(req.tenantName()));
        } catch (BizException e) {
            throw loginFailed(req.username(), request, e.getMessage());
        }
        try {
            passwordPolicy.checkLocked(req.username());
            // 验证码错误不计入密码错误次数
            CaptchaMode captcha = settings.getEnum(Setting.CAPTCHA_MODE, CaptchaMode.class);
            if (captcha == CaptchaMode.ALWAYS || captcha == CaptchaMode.AFTER_FAILURE && passwordPolicy.hasFailures(req.username())) {
                captchaService.verify(req.captchaId(), req.captchaCode());
            }
        } catch (BizException e) {
            throw loginFailed(req.username(), request, e.getMessage());
        }
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            // 不存在的用户名同样计数，避免借此探测账号是否存在
            int left = passwordPolicy.recordFailure(req.username());
            throw loginFailed(req.username(), request, left == 0 ? "用户名或密码错误，账号已锁定"
                    : left <= 2 ? "用户名或密码错误，再错 " + left + " 次将锁定账号" : "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw loginFailed(req.username(), request, "账号已停用");
        }
        passwordPolicy.clearFailures(req.username());
        String token = tokenService.createToken(user.getId(), user.getUsername(), request);
        publishLoginLog(user.getUsername(), request, 1, "登录成功");
        boolean mustChange = Integer.valueOf(1).equals(user.getPwdReset()) || passwordPolicy.isExpired(user.getPwdUpdateTime());
        return new LoginResponse(token, mustChange);
    }

    public void logout() {
        tokenService.removeSession(SecurityUtils.getLoginUser().tokenId());
    }

    public UserInfo currentUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = userMapper.selectById(loginUser.userId());
        return new UserInfo(user, loginUser.roles(), loginUser.perms(),
                tenantLookup.isEnabled() ? tenantLookup.currentTenantName() : null);
    }

    /**
     * 当前用户可见的菜单树（目录 + 菜单，不含按钮），前端据此生成动态路由
     */
    public List<SysMenu> currentUserMenus() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysMenu> menus = loginUser.isAdmin()
                ? menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .in(SysMenu::getType, SysMenu.TYPE_DIR, SysMenu.TYPE_MENU)
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSort))
                : menuMapper.selectRouterMenusByUserId(loginUser.userId(), tenantLookup.currentPackageId());
        boolean platform = TenantContext.isPlatform();
        boolean saas = tenantLookup.isEnabled();
        // 门户模块只对所属门户的账号显示，门户账号只显示所属门户；其余模块只对内部用户显示
        Map<Long, PortalKind> portalModules = new HashMap<>();
        menus.forEach(m -> {
            PortalKind k = PortalKind.ofModulePath(m.getPath());
            if (k != null) {
                portalModules.put(m.getId(), k);
            }
        });
        PortalKind kind = loginUser.portalKind();
        // 平台专属菜单只对平台租户显示，租户管理菜单只在 SaaS 模式下显示
        return TreeUtils.build(menus.stream()
                .filter(m -> platform || !PlatformFeatures.isPlatformOnly(m.getPerms()))
                .filter(m -> saas || !PlatformFeatures.isTenantManagement(m.getPerms()))
                .filter(m -> kind == portalModules.getOrDefault(m.getId(), portalModules.get(m.getParentId())))
                .toList());
    }

    private BizException loginFailed(String username, HttpServletRequest request, String msg) {
        publishLoginLog(username, request, 0, msg);
        return new BizException(msg);
    }

    private void publishLoginLog(String username, HttpServletRequest request, int status, String msg) {
        String ua = ServletUtils.getUserAgent(request);
        publisher.publishEvent(new LoginLogEvent(username, ServletUtils.getClientIp(request),
                UserAgentUtils.getBrowser(ua), UserAgentUtils.getOs(ua), status, msg, LocalDateTime.now()));
    }
}
