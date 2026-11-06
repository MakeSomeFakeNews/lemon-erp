package com.lemon.erp.modules.ai.model;

/** 对话消息的角色（ai_message.role） */
public enum AiMessageRole {
    /** 用户的提问 */
    USER,
    /** 模型的回答（可能带工具调用请求） */
    ASSISTANT,
    /** 工具的返回结果 */
    TOOL
}
