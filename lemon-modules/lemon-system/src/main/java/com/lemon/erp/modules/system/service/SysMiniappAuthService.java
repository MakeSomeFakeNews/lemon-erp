package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.system.dto.MiniappBindRequest;
import com.lemon.erp.modules.system.dto.MiniappLoginResult;
import com.lemon.erp.modules.system.dto.MiniappLoginResult.MiniappTenant;
import com.lemon.erp.modules.system.entity.SysMiniappUser;
import com.lemon.erp.modules.system.entity.SysTenant;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysMiniappUserMapper;
import com.lemon.erp.modules.system.mapper.SysTenantMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.miniapp.WeChatMiniClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 微信小程序登录：wx.login 的 code 换取 openid，按绑定关系登录；未绑定时用账号密码登录并绑定当前微信。
 * 同一个微信可以绑定不同租户的账号（SaaS 模式），绑定了多个时由用户选择租户；「使用其他账号登录」时不自动登录，登录后绑定新账号。
 * openid 不返回前端，登录过程中缓存在会话标识（10 分钟内有效）下
 */
@Service
@RequiredArgsConstructor
public class SysMiniappAuthService {

    public static final String SESSION_KEY = "mp:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final String LOGIN_LABEL = "system.label.miniappLogin";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WeChatMiniClient client;
    private final SysMiniappUserMapper miniappUserMapper;
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantLookup tenantLookup;
    private final AuthService authService;
    private final CacheService cache;

    /** 能登录的绑定：租户可用、用户存在且启用、不是门户账号 */
    private record Candidate(SysMiniappUser binding, SysUser user) {
    }

    /** @param switchAccount 不自动登录，返回会话标识以便用其他账号登录并绑定 */
    public MiniappLoginResult login(String code, boolean switchAccount) {
        String openid = client.openid(code);
        List<Candidate> candidates = switchAccount ? List.of() : candidates(openid);
        if (candidates.size() == 1) {
            return issue(candidates.get(0));
        }
        String key = newKey();
        cache.set(SESSION_KEY + key, openid, SESSION_TTL);
        List<MiniappTenant> tenants = candidates.stream()
                .map(c -> new MiniappTenant(c.binding().getTenantId(), tenantName(c.binding().getTenantId()))).toList();
        return new MiniappLoginResult(null, false, key, tenants);
    }

    /** 绑定了多个租户的账号时，选择租户登录 */
    public MiniappLoginResult choose(String key, Long tenantId) {
        String openid = requireOpenid(key);
        Candidate c = candidates(openid).stream().filter(x -> x.binding().getTenantId().equals(tenantId)).findFirst()
                .orElseThrow(() -> new BizException("system.error.miniappTenantNotBound"));
        cache.delete(SESSION_KEY + key);
        return issue(c);
    }

    /** 账号密码登录，成功后绑定当前微信（替换该微信在本租户的绑定、该用户之前绑定的微信） */
    @Transactional
    public MiniappLoginResult bind(MiniappBindRequest req) {
        String openid = requireOpenid(req.key());
        SysUser user = authService.authenticate(req.toLogin());
        // 门户账号、停用账号在这里报错，不会绑定
        MiniappLoginResult result = MiniappLoginResult.of(authService.loginAs(user, LOGIN_LABEL));
        miniappUserMapper.delete(Wrappers.<SysMiniappUser>lambdaQuery()
                .and(w -> w.eq(SysMiniappUser::getOpenid, openid).or().eq(SysMiniappUser::getUserId, user.getId())));
        SysMiniappUser binding = new SysMiniappUser();
        binding.setUserId(user.getId());
        binding.setOpenid(openid);
        miniappUserMapper.insert(binding);
        cache.delete(SESSION_KEY + req.key());
        return result;
    }

    /** 当前用户解除微信绑定（小程序中退出时可选） */
    public void unbindMine() {
        miniappUserMapper.delete(Wrappers.<SysMiniappUser>lambdaQuery().eq(SysMiniappUser::getUserId, SecurityUtils.getUserId()));
    }

    private List<Candidate> candidates(String openid) {
        List<Candidate> list = new ArrayList<>();
        for (SysMiniappUser b : miniappUserMapper.selectByOpenid(openid)) {
            if (!TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> tenantLookup.isActive(b.getTenantId()))) {
                continue;
            }
            SysUser user = TenantContext.callAs(b.getTenantId(), () -> userMapper.selectById(b.getUserId()));
            if (user != null && user.getStatus() == EnableStatus.ENABLED && user.getSupplierId() == null && user.getCustomerId() == null) {
                list.add(new Candidate(b, user));
            }
        }
        return list;
    }

    private MiniappLoginResult issue(Candidate c) {
        TenantContext.set(c.binding().getTenantId());
        return MiniappLoginResult.of(authService.loginAs(c.user(), LOGIN_LABEL));
    }

    private String requireOpenid(String key) {
        String openid = cache.get(SESSION_KEY + key, String.class);
        if (openid == null) {
            throw new BizException("system.error.miniappSessionExpired");
        }
        return openid;
    }

    private String tenantName(Long tenantId) {
        if (!tenantLookup.isEnabled()) {
            return null;
        }
        SysTenant t = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID, () -> tenantMapper.selectById(tenantId));
        return t == null ? null : t.getName();
    }

    private static String newKey() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
