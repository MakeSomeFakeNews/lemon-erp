package com.lemon.erp.common.exception;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.R;
import lombok.Getter;

/**
 * 业务异常，会以 R.fail 的形式返回给前端。
 * 消息写中文模板，参数用 {0} {1} 占位（如 {@code new BizException("登录名「{0}」已存在", username)}），
 * 返回时按请求语言翻译（{@link I18n}，译文在各模块的 resources/i18n/*.json）；getMessage 为中文
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final String template;
    private final transient Object[] args;

    public BizException(String template, Object... args) {
        this(R.ERROR, template, args);
    }

    public BizException(int code, String template, Object... args) {
        super(I18n.format(template, args));
        this.code = code;
        this.template = template;
        this.args = args;
    }

    /** 按当前请求的语言 */
    @Override
    public String getLocalizedMessage() {
        return I18n.t(template, args);
    }
}
