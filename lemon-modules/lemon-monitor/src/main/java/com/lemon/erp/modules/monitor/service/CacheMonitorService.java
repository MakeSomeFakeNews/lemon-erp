package com.lemon.erp.modules.monitor.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.modules.monitor.dto.CacheInfo;
import com.lemon.erp.modules.monitor.dto.CacheName;
import com.lemon.erp.modules.monitor.dto.CacheValue;
import com.lemon.erp.modules.system.im.ImHttp;
import com.lemon.erp.modules.system.miniapp.WeChatMiniClient;
import com.lemon.erp.modules.system.service.CaptchaService;
import com.lemon.erp.modules.system.service.PasswordPolicy;
import com.lemon.erp.modules.system.service.SysBizRuleService;
import com.lemon.erp.modules.system.service.SysConfigService;
import com.lemon.erp.modules.system.service.SysDictService;
import com.lemon.erp.modules.system.service.SysImAuthService;
import com.lemon.erp.modules.system.service.SysMiniappAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存监控：Redis 运行信息 + 应用缓存管理。
 * 缓存管理只开放应用自己使用的几类前缀，不能通过接口查看/删除任意 key。
 */
@Service
@RequiredArgsConstructor
public class CacheMonitorService {

    /** 应用使用的缓存，新增缓存类型时在这里登记即可出现在缓存列表中（名称、说明写消息键，登记在 monitor 资源包） */
    private static final List<CacheName> CACHES = List.of(
            new CacheName(TokenService.TOKEN_KEY, "monitor.label.loginSessions", "monitor.label.sessionsOnlineUsersAfterClearingAll", 0),
            new CacheName(TokenService.PERMS_KEY, "monitor.label.userPermissions", "monitor.label.rolesPermissionsSignedUsersReloadedAutomatically", 0),
            new CacheName(SysDictService.CACHE_KEY, "monitor.label.dictionaryData", "monitor.label.dictionaryDataCachedDictionaryType", 0),
            new CacheName(PasswordPolicy.FAIL_KEY, "monitor.label.loginFailures", "monitor.label.consecutiveLoginFailuresPerUsernameAccount", 0),
            new CacheName(CaptchaService.CACHE_KEY, "monitor.label.loginCaptcha", "monitor.label.captchaAnswersValidMinutesInvalidatedAfter", 0),
            new CacheName(SysConfigService.CACHE_KEY, "monitor.label.systemSettings", "monitor.label.settingsChangedSettingsClearedAutomaticallySave", 0),
            new CacheName(SysBizRuleService.CACHE_KEY, "monitor.label.businessRules", "monitor.label.changedBusinessRulesCachedPerTenant", 0),
            new CacheName(ImHttp.TOKEN_KEY, "monitor.label.integrationTokens", "monitor.label.accessTokenDingtalkWecomFeishuApps", 0),
            new CacheName(SysImAuthService.STATE_KEY, "monitor.label.thirdPartyAuthorization", "monitor.label.stateDingtalkWecomFeishuLoginBinding", 0),
            new CacheName(WeChatMiniClient.TOKEN_KEY, "monitor.label.miniappToken", "monitor.label.miniappTokenDesc", 0),
            new CacheName(SysMiniappAuthService.SESSION_KEY, "monitor.label.miniappSession", "monitor.label.miniappSessionDesc", 0));
    /** 单次最多返回的 key 数量，避免一次性返回过多 */
    private static final int MAX_KEYS = 1000;

    private final CacheService cache;
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Value("${lemon.cache.type:redis}")
    private String cacheType;

    public CacheInfo getInfo() {
        if (!isRedis()) {
            return new CacheInfo("memory", Map.of(), cache.keys("").size(), List.of());
        }
        StringRedisTemplate redis = redisProvider.getObject();
        return redis.execute((RedisCallback<CacheInfo>) conn -> {
            Map<String, String> info = toMap(conn.serverCommands().info());
            Long dbSize = conn.serverCommands().dbSize();
            return new CacheInfo("redis", info, dbSize == null ? 0 : dbSize, commandStats(conn));
        });
    }

    public List<CacheName> listNames() {
        return CACHES.stream()
                .map(c -> new CacheName(c.prefix(), Messages.get(c.name()), Messages.get(c.remark()), cache.keys(c.prefix()).size()))
                .toList();
    }

    public List<String> listKeys(String prefix) {
        checkPrefix(prefix);
        return cache.keys(prefix).stream().sorted().limit(MAX_KEYS).toList();
    }

    public CacheValue getValue(String key) {
        checkKey(key);
        String value = cache.getRaw(key);
        if (value == null) {
            throw new BizException("monitor.error.cacheEntryDoesNotExistHas");
        }
        return new CacheValue(key, value, cache.getExpire(key));
    }

    public void clearByPrefix(String prefix) {
        checkPrefix(prefix);
        cache.deleteByPrefix(prefix);
    }

    public void deleteKey(String key) {
        checkKey(key);
        cache.delete(key);
    }

    private boolean isRedis() {
        return "redis".equalsIgnoreCase(cacheType);
    }

    private void checkPrefix(String prefix) {
        if (CACHES.stream().noneMatch(c -> c.prefix().equals(prefix))) {
            throw new BizException("monitor.error.unsupportedCacheType", prefix);
        }
    }

    private void checkKey(String key) {
        if (key == null || CACHES.stream().noneMatch(c -> key.startsWith(c.prefix()))) {
            throw new BizException("monitor.error.unsupportedCacheKey", key);
        }
    }

    private static List<CacheInfo.CommandStat> commandStats(RedisConnection conn) {
        Properties props = conn.serverCommands().info("commandstats");
        List<CacheInfo.CommandStat> stats = new ArrayList<>();
        if (props == null) {
            return stats;
        }
        // cmdstat_get -> calls=10,usec=56,usec_per_call=5.60,rejected_calls=0,failed_calls=0
        props.forEach((k, v) -> {
            String name = String.valueOf(k);
            if (!name.startsWith("cmdstat_")) return;
            Map<String, String> fields = new HashMap<>();
            for (String pair : String.valueOf(v).split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) fields.put(kv[0], kv[1]);
            }
            stats.add(new CacheInfo.CommandStat(name.substring("cmdstat_".length()),
                    parseLong(fields.get("calls")), parseLong(fields.get("usec")),
                    parseDouble(fields.get("usec_per_call"))));
        });
        stats.sort(Comparator.comparingLong(CacheInfo.CommandStat::calls).reversed());
        return stats;
    }

    private static Map<String, String> toMap(Properties props) {
        Map<String, String> map = new HashMap<>();
        if (props != null) {
            props.forEach((k, v) -> map.put(String.valueOf(k), String.valueOf(v)));
        }
        return map;
    }

    private static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return s == null ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
