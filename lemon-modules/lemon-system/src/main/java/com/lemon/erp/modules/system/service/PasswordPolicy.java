package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.setting.PasswordComplexity;
import com.lemon.erp.framework.setting.Setting;
import com.lemon.erp.framework.setting.SettingService;
import com.lemon.erp.framework.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 密码策略与登录失败锁定（系统参数「密码策略」「登录与会话」）：
 * <ul>
 *   <li>新设密码（新增用户、重置、修改、开通账号）不少于最小长度并满足复杂度</li>
 *   <li>连续登录失败达到次数后锁定账号一段时间，锁定期间密码正确也不能登录；管理员重置密码后解锁</li>
 *   <li>密码超过有效天数时登录后提示修改（0 不限制）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    /** 登录失败计数 */
    public static final String FAIL_KEY = "login:fail:";

    private static final int MAX_LENGTH = 32;

    private final CacheService cache;
    private final SettingService settings;

    public void check(String password) {
        check(password, settings.getInt(Setting.PASSWORD_MIN_LENGTH),
                settings.getEnum(Setting.PASSWORD_COMPLEXITY, PasswordComplexity.class), "密码");
    }

    /** 按指定的策略校验（保存参数时用修改后的策略校验初始密码） */
    public static void check(String password, int minLength, PasswordComplexity complexity, String name) {
        if (password == null || password.length() < minLength || password.length() > MAX_LENGTH) {
            throw new BizException(name + "长度为 " + minLength + "-" + MAX_LENGTH + " 位");
        }
        if (!complexity.matches(password)) {
            throw new BizException(name + "必须" + complexity.requirement());
        }
    }

    /** 已锁定时抛出异常 */
    public void checkLocked(String username) {
        Integer fails = cache.get(failKey(username), Integer.class);
        if (fails != null && fails >= settings.getInt(Setting.LOGIN_MAX_RETRY)) {
            long seconds = Math.max(cache.getExpire(failKey(username)), 60);
            throw new BizException("密码错误次数过多，账号已锁定，请 " + (seconds + 59) / 60 + " 分钟后再试或联系管理员重置密码");
        }
    }

    /**
     * 记录一次失败
     *
     * @return 剩余可尝试次数，0 表示已锁定
     */
    public int recordFailure(String username) {
        String key = failKey(username);
        Integer fails = cache.get(key, Integer.class);
        int count = (fails == null ? 0 : fails) + 1;
        cache.set(key, count, Duration.ofMinutes(settings.getInt(Setting.LOGIN_LOCK_MINUTES)));
        return Math.max(settings.getInt(Setting.LOGIN_MAX_RETRY) - count, 0);
    }

    /** 是否有未清除的登录失败（「密码输错后」需要验证码） */
    public boolean hasFailures(String username) {
        Integer fails = cache.get(failKey(username), Integer.class);
        return fails != null && fails > 0;
    }

    public void clearFailures(String username) {
        cache.delete(failKey(username));
    }

    /** 用户名只在租户内唯一，其他租户的计数 key 带上租户 ID */
    private static String failKey(String username) {
        return TenantContext.isPlatform() ? FAIL_KEY + username : FAIL_KEY + TenantContext.require() + ":" + username;
    }

    /** 密码是否已过期（未记录修改时间的视为未过期） */
    public boolean isExpired(LocalDateTime pwdUpdateTime) {
        int expireDays = settings.getInt(Setting.PASSWORD_EXPIRE_DAYS);
        return expireDays > 0 && pwdUpdateTime != null && pwdUpdateTime.plusDays(expireDays).isBefore(LocalDateTime.now());
    }
}
