package com.lemon.erp.modules.ai.client;

import com.lemon.erp.common.exception.BizException;

/**
 * 调用模型失败：消息键说明原因（密钥错误、额度不足、模型不存在、超时等），按用户语言返回可读提示。
 * {@link #usage()} 是失败前已经消耗的用量（如 JSON 输出重试后仍不合格），调用方应照常记录
 */
public class LlmException extends BizException {

    private final transient LlmUsage usage;

    public LlmException(String messageKey, Object... args) {
        this(LlmUsage.ZERO, messageKey, args);
    }

    public LlmException(LlmUsage usage, String messageKey, Object... args) {
        super(messageKey, args);
        this.usage = usage == null ? LlmUsage.ZERO : usage;
    }

    public LlmUsage usage() {
        return usage;
    }
}
