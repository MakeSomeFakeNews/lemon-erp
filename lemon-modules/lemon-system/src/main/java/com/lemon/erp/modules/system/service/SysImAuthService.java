package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.dto.ImCallbackRequest;
import com.lemon.erp.modules.system.dto.ImCallbackResult;
import com.lemon.erp.modules.system.dto.ImMyBinding;
import com.lemon.erp.modules.system.dto.ImProvider;
import com.lemon.erp.modules.system.dto.LoginResponse;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.entity.SysImUser;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.im.ImIdentity;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.mapper.SysImUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 第三方授权登录与账号绑定（OAuth 授权码方式）：
 * 登录页或平台客户端内打开的入口先取授权页地址（state 记录租户、用途和登录后进入的页面，10 分钟内一次有效），
 * 平台授权后跳回前端回调页，回调页把授权码交给后端换取成员，按绑定关系登录或完成绑定
 */
@Service
@RequiredArgsConstructor
public class SysImAuthService {

    public static final String STATE_KEY = "im:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysImConfigService configService;
    private final SysImUserMapper imUserMapper;
    private final SysUserMapper userMapper;
    private final SysTenantLookup tenantLookup;
    private final AuthService authService;
    private final CacheService cache;

    /**
     * @param bind     绑定账号（userId 为当前用户），否则为登录
     * @param redirect 登录后进入的页面（站内路由）
     */
    public record State(Long tenantId, ImPlatform platform, boolean bind, Long userId, String redirect) {
    }

    /** 登录页可用的第三方登录方式（租户名称不存在时为空） */
    public List<ImProvider> providers(String tenantName) {
        Long tenantId;
        try {
            TenantContext.set(TenantContext.PLATFORM_TENANT_ID);
            tenantId = tenantLookup.resolveLoginTenant(tenantName);
        } catch (BizException e) {
            return List.of();
        }
        TenantContext.set(tenantId);
        return configService.enabledList().stream()
                .filter(c -> Boolean.TRUE.equals(c.getLoginEnabled()))
                .sorted((a, b) -> a.getPlatform().compareTo(b.getPlatform()))
                .map(c -> new ImProvider(c.getPlatform(), c.getPlatform().label())).toList();
    }

    /**
     * 登录的授权页地址
     *
     * @param tenantName 登录页填写的租户（SaaS 模式）
     * @param tenantId   平台客户端内的入口链接带租户 ID（消息链接、工作台应用首页）
     * @param inApp      在平台客户端内打开
     */
    public String loginUrl(ImPlatform platform, String tenantName, Long tenantId, boolean inApp, String redirect) {
        TenantContext.set(TenantContext.PLATFORM_TENANT_ID);
        Long tenant;
        if (!tenantLookup.isEnabled()) {
            tenant = TenantContext.PLATFORM_TENANT_ID;
        } else if (tenantId != null) {
            if (!tenantLookup.isActive(tenantId)) {
                throw new BizException("租户不存在或已停用");
            }
            tenant = tenantId;
        } else {
            tenant = tenantLookup.resolveLoginTenant(tenantName);
        }
        TenantContext.set(tenant);
        SysImConfig c = configService.requireEnabled(platform);
        if (!Boolean.TRUE.equals(c.getLoginEnabled())) {
            throw new BizException("未开启" + platform.label() + "登录");
        }
        return authorizeUrl(c, new State(tenant, platform, false, null, safeRedirect(redirect)), inApp);
    }

    /** 当前用户绑定账号的授权页地址 */
    public String bindUrl(ImPlatform platform) {
        SysImConfig c = configService.requireEnabled(platform);
        return authorizeUrl(c, new State(TenantContext.require(), platform, true, SecurityUtils.getUserId(), null), false);
    }

    private String authorizeUrl(SysImConfig c, State state, boolean inApp) {
        if (!StringUtils.hasText(c.getHomeUrl())) {
            throw new BizException("请先在第三方集成中填写系统访问地址");
        }
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String key = HexFormat.of().formatHex(bytes);
        cache.set(STATE_KEY + key, state, STATE_TTL);
        return configService.client(c.getPlatform()).authorizeUrl(c, redirectUri(c), key, inApp);
    }

    private static String redirectUri(SysImConfig c) {
        return c.getHomeUrl() + SysImConfigService.CALLBACK_PATH;
    }

    /** 站内路由才允许跳转（避免借登录跳到外部网站） */
    private static String safeRedirect(String redirect) {
        return redirect != null && redirect.startsWith("/") && !redirect.startsWith("//") && !redirect.contains("\\")
                && redirect.length() <= 255 ? redirect : null;
    }

    /** 授权回调：登录返回令牌，绑定则保存绑定关系 */
    public ImCallbackResult callback(ImCallbackRequest req) {
        String key = STATE_KEY + req.state();
        State state = cache.get(key, State.class);
        if (state == null) {
            throw new BizException("授权已过期，请重新操作");
        }
        cache.delete(key);
        TenantContext.set(state.tenantId());
        ImPlatform p = state.platform();
        SysImConfig c = configService.requireEnabled(p);
        ImIdentity identity = configService.client(p).identify(c, req.code(), redirectUri(c));
        if (state.bind()) {
            bind(state.userId(), p, identity.userId(), identity.name());
            return new ImCallbackResult(p, true, null, false, null);
        }
        SysImUser binding = imUserMapper.selectOne(Wrappers.<SysImUser>lambdaQuery()
                .eq(SysImUser::getPlatform, p).eq(SysImUser::getImUserId, identity.userId()));
        SysUser user = binding == null ? null : userMapper.selectById(binding.getUserId());
        if (user == null) {
            String msg = "该" + p.label() + "账号（" + identity.userId() + "）还没有绑定系统用户，请先用账号密码登录，在个人中心绑定，或联系管理员同步通讯录";
            String name = p.label() + ":" + identity.userId();
            authService.recordLoginFailure(name.length() > 64 ? name.substring(0, 64) : name, "该" + p.label() + "账号未绑定系统用户");
            throw new BizException(msg);
        }
        LoginResponse login = authService.loginAs(user, p.label() + "登录");
        return new ImCallbackResult(p, false, login.token(), login.passwordExpired(), state.redirect());
    }

    /** 保存绑定：成员已绑定其他用户时报错，用户已绑定其他成员时改为新成员 */
    void bind(Long userId, ImPlatform platform, String imUserId, String imName) {
        SysImUser other = imUserMapper.selectOne(Wrappers.<SysImUser>lambdaQuery()
                .eq(SysImUser::getPlatform, platform).eq(SysImUser::getImUserId, imUserId));
        if (other != null && !other.getUserId().equals(userId)) {
            SysUser u = userMapper.selectById(other.getUserId());
            throw new BizException("该" + platform.label() + "账号已绑定用户「" + (u == null ? other.getUserId() : u.getUsername()) + "」，请先解除绑定");
        }
        SysImUser mine = imUserMapper.selectOne(Wrappers.<SysImUser>lambdaQuery()
                .eq(SysImUser::getUserId, userId).eq(SysImUser::getPlatform, platform));
        if (mine == null) {
            mine = new SysImUser();
            mine.setUserId(userId);
            mine.setPlatform(platform);
            mine.setImUserId(imUserId);
            mine.setImName(imName);
            imUserMapper.insert(mine);
        } else {
            mine.setImUserId(imUserId);
            mine.setImName(imName);
            imUserMapper.updateById(mine);
        }
    }

    /** 个人中心：已启用的平台及本人的绑定 */
    public List<ImMyBinding> myBindings() {
        Map<ImPlatform, SysImUser> mine = imUserMapper.selectList(Wrappers.<SysImUser>lambdaQuery()
                        .eq(SysImUser::getUserId, SecurityUtils.getUserId())).stream()
                .collect(Collectors.toMap(SysImUser::getPlatform, Function.identity()));
        return configService.enabledList().stream()
                .sorted((a, b) -> a.getPlatform().compareTo(b.getPlatform()))
                .map(c -> {
                    SysImUser b = mine.get(c.getPlatform());
                    return new ImMyBinding(c.getPlatform(), c.getPlatform().label(), b != null,
                            b == null ? null : b.getImUserId(), b == null ? null : b.getImName(), b == null ? null : b.getCreateTime());
                }).toList();
    }

    public void unbindMine(ImPlatform platform) {
        imUserMapper.delete(Wrappers.<SysImUser>lambdaQuery()
                .eq(SysImUser::getUserId, SecurityUtils.getUserId()).eq(SysImUser::getPlatform, platform));
    }
}
