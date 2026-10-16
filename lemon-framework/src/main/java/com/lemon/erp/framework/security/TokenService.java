package com.lemon.erp.framework.security;

import com.lemon.erp.common.util.ServletUtils;
import com.lemon.erp.common.util.UserAgentUtils;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.ip.IpLocationService;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.framework.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 登录会话与登录用户权限缓存管理
 * <ul>
 *     <li>login:token:{tokenId} → OnlineSession，TTL 与 JWT 过期时间一致，删除即注销/强退</li>
 *     <li>login:perms:{userId} → LoginUser（角色+权限+数据权限），30 分钟，用户/角色/菜单/部门变更提交后清理</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String TOKEN_KEY = "login:token:";
    public static final String PERMS_KEY = "login:perms:";
    private static final Duration PERMS_TTL = Duration.ofMinutes(30);

    private final CacheService cache;
    private final JwtTokenProvider tokenProvider;
    private final LoginUserLoader loginUserLoader;
    private final IpLocationService ipLocationService;
    private final SettingService settings;

    /** 创建会话并签发 token；开启「同一账号只允许一处登录」时，先让该账号之前的会话下线 */
    public String createToken(Long userId, String username, HttpServletRequest request) {
        if (settings.getBool(Setting.SESSION_SINGLE)) {
            listSessions().stream()
                    .filter(s -> Objects.equals(s.userId(), userId))
                    .forEach(s -> removeSession(s.tokenId()));
        }
        Duration expire = Duration.ofHours(settings.getInt(Setting.SESSION_EXPIRE_HOURS));
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String ua = ServletUtils.getUserAgent(request);
        String ip = ServletUtils.getClientIp(request);
        OnlineSession session = new OnlineSession(tokenId, userId, username, ip, ipLocationService.getLocation(ip),
                UserAgentUtils.getBrowser(ua), UserAgentUtils.getOs(ua), LocalDateTime.now(), TenantContext.require());
        cache.set(TOKEN_KEY + tokenId, session, expire);
        return tokenProvider.createToken(tokenId, userId, username, expire);
    }

    /** 根据 token 获取有效会话；token 无效或会话已注销返回 null */
    public OnlineSession getSession(String token) {
        String tokenId = tokenProvider.parseTokenId(token);
        return tokenId == null ? null : cache.get(TOKEN_KEY + tokenId, OnlineSession.class);
    }

    public void removeSession(String tokenId) {
        cache.delete(TOKEN_KEY + tokenId);
    }

    /** 在线会话：平台租户看到全部，其他租户只看到本租户的 */
    public List<OnlineSession> listSessions() {
        // SCAN 取 key 后一次 MGET 批量读取，避免逐个 GET 的 N 次往返
        Long tenantId = TenantContext.get();
        boolean all = tenantId == null || TenantContext.isPlatform();
        return cache.getAll(List.copyOf(cache.keys(TOKEN_KEY)), OnlineSession.class).stream()
                .filter(Objects::nonNull)
                .filter(s -> all || tenantId.equals(s.tenantId()))
                .sorted(Comparator.comparing(OnlineSession::loginTime).reversed())
                .toList();
    }

    /** 在线会话数；平台租户只统计 key 数量，不读取会话内容 */
    public long countSessions() {
        return TenantContext.get() == null || TenantContext.isPlatform()
                ? cache.keys(TOKEN_KEY).size() : listSessions().size();
    }

    /** 踢掉租户的全部会话（租户停用、过期时） */
    public void kickTenant(Long tenantId) {
        cache.getAll(List.copyOf(cache.keys(TOKEN_KEY)), OnlineSession.class).stream()
                .filter(s -> s != null && tenantId.equals(s.tenantId()))
                .forEach(s -> {
                    removeSession(s.tokenId());
                    evictLoginUser(s.userId());
                });
    }

    /** 踢掉指定用户的全部会话 */
    public void kickUser(Long userId) {
        listSessions().stream()
                .filter(s -> Objects.equals(s.userId(), userId))
                .forEach(s -> removeSession(s.tokenId()));
        evictLoginUser(userId);
    }

    /** 加载登录用户（带缓存）；用户不存在或停用返回 null */
    public LoginUser getLoginUser(Long userId) {
        String key = PERMS_KEY + userId;
        LoginUser cached = cache.get(key, LoginUser.class);
        // dataScope / tenantId 为 null 说明是升级前缓存的旧结构，重新加载
        if (cached != null && cached.dataScope() != null && cached.tenantId() != null) {
            return cached;
        }
        LoginUser loaded = loginUserLoader.loadLoginUser(userId);
        if (loaded != null) {
            cache.set(key, loaded, PERMS_TTL);
        }
        return loaded;
    }

    public void evictLoginUser(Long userId) {
        afterCommit(() -> cache.delete(PERMS_KEY + userId));
    }

    /** 角色、菜单变更影响面广，直接清空全部权限缓存 */
    public void evictAllLoginUsers() {
        afterCommit(() -> cache.deleteByPrefix(PERMS_KEY));
    }

    /**
     * 有事务时在提交后执行，没有事务时立即执行。
     * 如果在提交前清理缓存，并发请求可能把尚未提交的旧权限重新加载进缓存，直到缓存过期才更新。
     */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
