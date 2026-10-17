package com.lemon.erp.common.exception;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.R;
import lombok.Getter;

/**
 * 业务异常，会以 R.fail 的形式返回给前端。
 * 消息是国际化资源包中的键加参数（如 {@code new BizException("system.error.loginNameAlreadyExists", username)}，见 {@link Messages}），
 * 返回时按请求语言取消息；getMessage 为默认语言（中文），用于日志
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final String messageKey;
    private final transient Object[] args;

    public BizException(String messageKey, Object... args) {
        this(R.ERROR, messageKey, args);
    }

    public BizException(int code, String messageKey, Object... args) {
        super(Messages.get(Messages.DEFAULT_LOCALE, messageKey, args));
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    /** 按当前请求的语言 */
    @Override
    public String getLocalizedMessage() {
        return Messages.get(messageKey, args);
    }
}
