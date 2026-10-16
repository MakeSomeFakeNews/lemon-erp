package com.lemon.erp.framework.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lemon.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public <T> T get(String key, Class<T> type) {
        String json = redis.opsForValue().get(key);
        return json == null ? null : objectMapper.readValue(json, type);
    }

    @Override
    public <T> List<T> getAll(List<String> keys, Class<T> type) {
        if (keys.isEmpty()) {
            return List.of();
        }
        List<String> values = redis.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.nCopies(keys.size(), null);
        }
        return values.stream().map(json -> json == null ? null : objectMapper.readValue(json, type)).toList();
    }

    @Override
    public String getRaw(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public long getExpire(String key) {
        Long ttl = redis.getExpire(key);
        return ttl == null ? -2 : ttl;
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        String json = objectMapper.writeValueAsString(value);
        if (ttl == null) {
            redis.opsForValue().set(key, json);
        } else {
            redis.opsForValue().set(key, json, ttl);
        }
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public Set<String> keys(String prefix) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(500).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }
}
