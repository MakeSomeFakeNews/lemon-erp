package com.lemon.erp.common.exception;

/**
 * 枚举取值不合法（如 {@code EnableStatus} 收到 1 / 0 以外的值）。
 * JSON 反序列化时被 Jackson 包装，由 {@link GlobalExceptionHandler} 提示字段和取值。
 */
public class InvalidEnumValueException extends IllegalArgumentException {

    private final transient Object value;

    public InvalidEnumValueException(Class<? extends Enum<?>> type, Object value) {
        super("Invalid " + type.getSimpleName() + " value: " + value);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
