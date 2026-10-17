package com.lemon.erp.modules.monitor.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.modules.monitor.dto.CacheInfo;
import com.lemon.erp.modules.monitor.dto.CacheName;
import com.lemon.erp.modules.monitor.dto.CacheValue;
import com.lemon.erp.modules.system.im.ImHttp;
import com.lemon.erp.modules.system.service.CaptchaService;
import com.lemon.erp.modules.system.service.PasswordPolicy;
import com.lemon.erp.modules.system.service.SysBizRuleService;
import com.lemon.erp.modules.system.service.SysConfigService;
import com.lemon.erp.modules.system.service.SysDictService;
import com.lemon.erp.modules.system.service.SysImAuthService;
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

    /** 应用使用的缓存，新增缓存类型时在这里登记即可出现在缓存列表中 */
    private static final List<CacheName> CACHES = List.of(
            new CacheName(TokenService.TOKEN_KEY, "登录会话", "在线用户的登录会话，清空后所有用户需重新登录", 0),
            new CacheName(TokenService.PERMS_KEY, "用户权限", "登录用户的角色与权限，清空后下次请求自动重新加载", 0),
            new CacheName(SysDictService.CACHE_KEY, "字典数据", "按字典类型缓存的字典数据", 0),
            new CacheName(PasswordPolicy.FAIL_KEY, "登录失败", "按用户名记录的连续登录失败次数，达到上限时锁定账号；清空即解除锁定", 0),
            new CacheName(CaptchaService.CACHE_KEY, "登录验证码", "验证码答案，2 分钟内有效，校验一次后作废", 0),
            new CacheName(SysConfigService.CACHE_KEY, "系统参数", "参数设置中修改过的参数，保存时自动清理", 0),
            new CacheName(SysBizRuleService.CACHE_KEY, "业务参数", "按租户缓存的业务参数（修改过的），保存时自动清理", 0),
            new CacheName(ImHttp.TOKEN_KEY, "第三方集成令牌", "钉钉、企业微信、飞书应用的 access_token，按租户和平台缓存，修改集成配置时自动清理", 0),
            new CacheName(SysImAuthService.STATE_KEY, "第三方授权", "钉钉、企业微信、飞书授权登录 / 绑定的 state，10 分钟内有效，使用一次后作废", 0));
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
                .map(c -> new CacheName(c.prefix(), I18n.t(c.name()), I18n.t(c.remark()), cache.keys(c.prefix()).size()))
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
            throw new BizException("缓存不存在或已过期");
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
            throw new BizException("不支持的缓存类型：{0}", prefix);
        }
    }

    private void checkKey(String key) {
        if (key == null || CACHES.stream().noneMatch(c -> key.startsWith(c.prefix()))) {
            throw new BizException("不支持的缓存键：{0}", key);
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
