package com.lemon.erp.modules.ai.model;

/**
 * 大模型服务商：都使用 OpenAI Chat Completions 兼容协议（POST {baseUrl}/chat/completions）。
 * 只内置默认接口地址，模型名称由用户按服务商控制台填写（不写死在代码里）
 */
public enum AiProvider {
    /** DeepSeek（深度求索） */
    DEEPSEEK("https://api.deepseek.com"),
    /** 通义千问（阿里云百炼，兼容模式） */
    QWEN("https://dashscope.aliyuncs.com/compatible-mode/v1"),
    /** 智谱 GLM（智谱开放平台） */
    ZHIPU("https://open.bigmodel.cn/api/paas/v4"),
    /** Kimi（月之暗面） */
    MOONSHOT("https://api.moonshot.cn/v1"),
    /** 豆包（火山方舟） */
    DOUBAO("https://ark.cn-beijing.volces.com/api/v3"),
    /** 其他兼容 OpenAI 协议的服务（私有化部署等），需要填写接口地址 */
    CUSTOM(null);

    private final String defaultBaseUrl;

    AiProvider(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /** 默认接口地址（不含 /chat/completions），CUSTOM 为 null */
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }
}
