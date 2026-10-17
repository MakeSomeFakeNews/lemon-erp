package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
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
import com.lemon.erp.modules.system.model.MenuType;
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
        // 未修改系统名称时按语言显示默认名称
        String systemName = settings.get(Setting.SYSTEM_NAME);
        if (Setting.SYSTEM_NAME.defaultValue().equals(systemName)) {
            systemName = Messages.get("framework.label.defaultSystemName");
        }
        return new AuthConfig(tenantLookup.isEnabled(), systemName, settings.getBool(Setting.SHOW_DEMO_ACCOUNT),
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
            throw loginFailed(req.username(), request, e.getMessageKey(), e.getArgs());
        }
        try {
            passwordPolicy.checkLocked(req.username());
            // 验证码错误不计入密码错误次数
            CaptchaMode captcha = settings.getEnum(Setting.CAPTCHA_MODE, CaptchaMode.class);
            if (captcha == CaptchaMode.ALWAYS || captcha == CaptchaMode.AFTER_FAILURE && passwordPolicy.hasFailures(req.username())) {
                captchaService.verify(req.captchaId(), req.captchaCode());
            }
        } catch (BizException e) {
            throw loginFailed(req.username(), request, e.getMessageKey(), e.getArgs());
        }
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            // 不存在的用户名同样计数，避免借此探测账号是否存在
            int left = passwordPolicy.recordFailure(req.username());
            if (left == 0) {
                throw loginFailed(req.username(), request, "system.error.wrongUsernamePasswordAccountLocked");
            }
            throw left <= 2 ? loginFailed(req.username(), request, "system.error.wrongUsernamePasswordAccountWillLocked", left)
                    : loginFailed(req.username(), request, "system.error.wrongUsernamePassword");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw loginFailed(req.username(), request, "system.error.accountDisabled");
        }
        passwordPolicy.clearFailures(req.username());
        return issue(user, request, "登录成功"); // i18n-ignore 登录日志
    }

    /**
     * 第三方授权登录（钉钉、企业微信、飞书）：平台已确认身份并按绑定关系找到用户，校验状态后签发令牌。调用前须设置租户
     *
     * @param viaKey 登录方式的消息键，如「钉钉登录」；登录日志记默认语言（中文）
     */
    public LoginResponse loginAs(SysUser user, String viaKey) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw loginFailed(user.getUsername(), request, "system.error.accountDisabled");
        }
        if (user.getSupplierId() != null || user.getCustomerId() != null) {
            // 登录日志记中文，提示按请求语言
            String via = Messages.get(Messages.DEFAULT_LOCALE, viaKey);
            publishLoginLog(user.getUsername(), request, 0, "门户账号不能使用" + via); // i18n-ignore
            throw new BizException("system.error.portalAccountsCannotUse", Messages.get(viaKey));
        }
        return issue(user, request, Messages.get(Messages.DEFAULT_LOCALE, viaKey) + "成功"); // i18n-ignore 登录日志
    }

    /** 第三方授权登录失败（如成员未绑定用户）记入登录日志 */
    public void recordLoginFailure(String username, String msg) {
        publishLoginLog(username, ServletUtils.getRequest(), 0, msg);
    }

    private LoginResponse issue(SysUser user, HttpServletRequest request, String msg) {
        String token = tokenService.createToken(user.getId(), user.getUsername(), request);
        publishLoginLog(user.getUsername(), request, 1, msg);
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
                        .in(SysMenu::getType, MenuType.M, MenuType.C)
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

    /** 登录失败：登录日志记默认语言（中文），返回的异常按请求语言 */
    private BizException loginFailed(String username, HttpServletRequest request, String messageKey, Object... args) {
        BizException e = new BizException(messageKey, args);
        publishLoginLog(username, request, 0, e.getMessage());
        return e;
    }

    private void publishLoginLog(String username, HttpServletRequest request, int status, String msg) {
        String ua = ServletUtils.getUserAgent(request);
        publisher.publishEvent(new LoginLogEvent(username, ServletUtils.getClientIp(request),
                UserAgentUtils.getBrowser(ua), UserAgentUtils.getOs(ua), status, msg, LocalDateTime.now()));
    }
}
