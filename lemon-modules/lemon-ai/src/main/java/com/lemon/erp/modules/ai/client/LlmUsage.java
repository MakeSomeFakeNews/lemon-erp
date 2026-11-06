package com.lemon.erp.modules.ai.client;

/**
 * token 用量
 *
 * @param promptTokens     输入
 * @param completionTokens 输出
 * @param estimated        服务商没有返回用量、按字符数估算的
 */
public record LlmUsage(int promptTokens, int completionTokens, boolean estimated) {

    public static final LlmUsage ZERO = new LlmUsage(0, 0, false);

    public LlmUsage(int promptTokens, int completionTokens) {
        this(promptTokens, completionTokens, false);
    }

    public int total() {
        return promptTokens + completionTokens;
    }

    public LlmUsage plus(LlmUsage other) {
        if (other == null) {
            return this;
        }
        return new LlmUsage(promptTokens + other.promptTokens, completionTokens + other.completionTokens, estimated || other.estimated);
    }
}
