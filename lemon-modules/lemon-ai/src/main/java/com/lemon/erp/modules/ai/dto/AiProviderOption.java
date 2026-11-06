package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.modules.ai.model.AiProvider;

/**
 * 服务商及其默认接口地址（前端下拉框和占位提示）
 *
 * @param value          服务商
 * @param defaultBaseUrl 默认接口地址，CUSTOM 为 null
 */
public record AiProviderOption(AiProvider value, String defaultBaseUrl) {
}
