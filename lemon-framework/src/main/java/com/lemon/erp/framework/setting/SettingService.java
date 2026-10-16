package com.lemon.erp.framework.setting;

/**
 * 读取系统参数（由系统管理模块实现，带缓存）：已修改的取保存的值，否则取默认值
 */
public interface SettingService {

    String get(Setting setting);

    default int getInt(Setting setting) {
        return Integer.parseInt(get(setting));
    }

    default boolean getBool(Setting setting) {
        return Boolean.parseBoolean(get(setting));
    }

    default <E extends Enum<E>> E getEnum(Setting setting, Class<E> type) {
        return Enum.valueOf(type, get(setting));
    }
}
