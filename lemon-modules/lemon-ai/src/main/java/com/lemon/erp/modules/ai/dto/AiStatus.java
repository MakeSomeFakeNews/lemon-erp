package com.lemon.erp.modules.ai.dto;

import com.lemon.erp.modules.ai.model.AiFeature;

import java.util.Map;

/**
 * 当前租户的 AI 可用状态（业务页面据此显示引导）
 *
 * @param configured 已填写服务商、API Key 和对话模型
 * @param enabled    总开关已开启
 * @param vision     已配置识图模型
 * @param features   各功能是否可用（已配置、已开启且功能开关打开）
 */
public record AiStatus(boolean configured, boolean enabled, boolean vision, Map<AiFeature, Boolean> features) {
}
