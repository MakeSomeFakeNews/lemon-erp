package com.lemon.erp.common.exception;

import com.lemon.erp.common.result.R;
import lombok.Getter;

/**
 * 业务异常，会以 R.fail 的形式返回给前端
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(R.ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
