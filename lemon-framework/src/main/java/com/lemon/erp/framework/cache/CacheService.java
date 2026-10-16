package com.lemon.erp.framework.cache;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 缓存抽象，值以 JSON 存储。实现：Redis（默认）/ 内存（lemon.cache.type=memory）
 */
public interface CacheService {

    <T> T get(String key, Class<T> type);

    /** 批量获取，结果与 keys 顺序一致，不存在的为 null。Redis 实现为一次 MGET */
    default <T> List<T> getAll(List<String> keys, Class<T> type) {
        return keys.stream().map(key -> get(key, type)).toList();
    }

    /** 原始 JSON 字符串，不存在返回 null（缓存监控查看用） */
    String getRaw(String key);

    /** 剩余过期时间（秒）：-1 永不过期，-2 不存在 */
    long getExpire(String key);

    /** ttl 为 null 表示不过期 */
    void set(String key, Object value, Duration ttl);

    void delete(String key);

    /** 列出指定前缀的所有 key */
    Set<String> keys(String prefix);

    default void deleteByPrefix(String prefix) {
        keys(prefix).forEach(this::delete);
    }
}
