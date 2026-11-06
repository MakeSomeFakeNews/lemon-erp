package com.lemon.erp.modules.ai.client;

import java.util.List;

/**
 * 对话中的一条消息。用静态方法创建：{@link #system}、{@link #user}、{@link #assistant}、{@link #tool}
 *
 * @param role       角色
 * @param content    文字（工具消息为结果 JSON）
 * @param images     用户消息附带的图片（识图），没有时为空列表
 * @param toolCalls  模型消息中请求调用的函数，没有时为空列表
 * @param toolCallId 工具消息对应的调用 ID
 */
public record LlmMessage(Role role, String content, List<LlmImage> images, List<LlmToolCall> toolCalls, String toolCallId) {

    /** 消息角色，序列化为小写（system、user、assistant、tool） */
    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL;

        public String wire() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public LlmMessage {
        images = images == null ? List.of() : List.copyOf(images);
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content, null, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content, null, null, null);
    }

    /** 带图片的用户消息（需使用识图模型） */
    public static LlmMessage user(String content, List<LlmImage> images) {
        return new LlmMessage(Role.USER, content, images, null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content, null, null, null);
    }

    /** 带工具调用请求的模型消息（回传工具结果前要先放入这条） */
    public static LlmMessage assistant(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage(Role.ASSISTANT, content, null, toolCalls, null);
    }

    /** 工具结果 */
    public static LlmMessage tool(String toolCallId, String resultJson) {
        return new LlmMessage(Role.TOOL, resultJson, null, null, toolCallId);
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }
}
