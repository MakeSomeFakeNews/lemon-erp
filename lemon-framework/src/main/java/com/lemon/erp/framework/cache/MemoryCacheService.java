package com.lemon.erp.framework.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 进程内缓存，同样以 JSON 存储以保持与 Redis 一致的语义；仅适合单实例/本地体验/测试
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lemon.cache.type", havingValue = "memory")
public class MemoryCacheService implements CacheService {

    private record Entry(String json, long expireAt) {
        boolean expired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public <T> T get(String key, Class<T> type) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expired()) {
            store.remove(key);
            return null;
        }
        return objectMapper.readValue(entry.json(), type);
    }

    @Override
    public String getRaw(String key) {
        Entry entry = store.get(key);
        return entry == null || entry.expired() ? null : entry.json();
    }

    @Override
    public long getExpire(String key) {
        Entry entry = store.get(key);
        if (entry == null || entry.expired()) return -2;
        return entry.expireAt() == 0 ? -1 : Math.max(0, (entry.expireAt() - System.currentTimeMillis()) / 1000);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        long expireAt = ttl == null ? 0 : System.currentTimeMillis() + ttl.toMillis();
        store.put(key, new Entry(objectMapper.writeValueAsString(value), expireAt));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public Set<String> keys(String prefix) {
        store.entrySet().removeIf(e -> e.getValue().expired());
        return store.keySet().stream().filter(k -> k.startsWith(prefix)).collect(Collectors.toSet());
    }
}
