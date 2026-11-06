package com.lemon.erp.modules.ai.client;

import java.math.BigDecimal;

/**
 * 调用一个模型所需的连接信息与默认参数（由租户的 AI 设置生成，见 {@code AiModelService#endpoint}）
 *
 * @param baseUrl     接口地址（不含 /chat/completions），如 https://api.deepseek.com
 * @param apiKey      API Key（Bearer）
 * @param model       模型名称
 * @param temperature 默认温度，为 null 时由服务商决定
 * @param maxTokens   默认的单次回答最大 token 数，为 null 时由服务商决定
 */
public record LlmEndpoint(String baseUrl, String apiKey, String model, BigDecimal temperature, Integer maxTokens) {

    /** 换一个模型（如识图模型），其他参数不变 */
    public LlmEndpoint withModel(String otherModel) {
        return new LlmEndpoint(baseUrl, apiKey, otherModel, temperature, maxTokens);
    }

    @Override
    public String toString() {
        // 不输出 API Key
        return "LlmEndpoint[baseUrl=" + baseUrl + ", model=" + model + "]";
    }
}
