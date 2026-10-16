package com.lemon.erp.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int ERROR = 500;

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return new R<>(OK, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(OK, "success", data);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(ERROR, msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}
