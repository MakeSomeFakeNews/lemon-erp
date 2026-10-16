package com.lemon.erp.modules.monitor.dto;

/**
 * @param ttl 剩余过期时间（秒），-1 永不过期
 */
public record CacheValue(String key, String value, long ttl) {
}
