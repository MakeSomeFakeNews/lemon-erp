package com.lemon.erp.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lemon.erp.common.exception.InvalidEnumValueException;

/**
 * 启用 / 停用状态（字典 sys_common_status）。
 * <p>
 * 与其他按名称存储的枚举不同，数据库、JSON 中都是数字 1 / 0（与字典取值、已有数据、前端一致）：
 * MyBatis-Plus 的默认枚举处理器（CompositeEnumTypeHandler）识别 {@link EnumValue} 按 code 读写；
 * Jackson 按 {@link JsonValue} 输出 code，按 {@link #fromJson(Object)} 从数字（兼容字符串 "1" / "0"）反序列化；
 * 查询参数 {@code ?status=1} 由 framework 的 WebMvcConfig 注册的转换器处理。
 */
public enum EnableStatus {
    /** 启用 */
    ENABLED(1),
    /** 停用 */
    DISABLED(0);

    @EnumValue
    @JsonValue
    private final int code;

    EnableStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }

    /** 按 code 取值，null 返回 null，不是 1 / 0 时抛出 {@link InvalidEnumValueException} */
    public static EnableStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (EnableStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new InvalidEnumValueException(EnableStatus.class, code);
    }

    public static EnableStatus of(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }

    /** 状态为启用（null 视为未启用） */
    public static boolean isEnabled(EnableStatus status) {
        return status == ENABLED;
    }

    /**
     * 从 JSON / 查询参数反序列化：数字 1 / 0，或字符串 "1" / "0"（也接受常量名 ENABLED / DISABLED），空字符串为 null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnableStatus fromJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            long n = ((Number) value).longValue();
            if (n == 1) {
                return ENABLED;
            }
            if (n == 0) {
                return DISABLED;
            }
        } else if (value instanceof String s) {
            String text = s.trim();
            if (text.isEmpty()) {
                return null;
            }
            switch (text) {
                case "1", "ENABLED" -> {
                    return ENABLED;
                }
                case "0", "DISABLED" -> {
                    return DISABLED;
                }
                default -> {
                    // 非法值，下方抛出
                }
            }
        }
        throw new InvalidEnumValueException(EnableStatus.class, value);
    }
}
