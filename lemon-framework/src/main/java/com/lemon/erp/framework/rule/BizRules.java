package com.lemon.erp.framework.rule;

/**
 * 读取当前租户的业务参数（由系统模块实现，按租户缓存）
 */
public interface BizRules {

    /** 参数值，未修改时为默认值；参数未登记时抛异常 */
    String get(String key);

    default boolean bool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    default int number(String key) {
        return Integer.parseInt(get(key));
    }

    default <E extends Enum<E>> E option(String key, Class<E> type) {
        return Enum.valueOf(type, get(key));
    }
}
