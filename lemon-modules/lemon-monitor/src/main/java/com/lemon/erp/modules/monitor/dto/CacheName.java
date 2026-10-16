package com.lemon.erp.modules.monitor.dto;

/**
 * 应用使用的一类缓存
 *
 * @param prefix key 前缀
 * @param name   名称
 * @param remark 说明
 * @param count  当前 key 数量
 */
public record CacheName(String prefix, String name, String remark, long count) {
}
