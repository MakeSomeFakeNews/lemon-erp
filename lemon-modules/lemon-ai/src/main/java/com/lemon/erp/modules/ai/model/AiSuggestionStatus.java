package com.lemon.erp.modules.ai.model;

/** 数据整理建议的处理状态（ai_cleanse_suggestion.status） */
public enum AiSuggestionStatus {
    /** 待处理 */
    PENDING,
    /** 已采纳（已修改或停用业务数据） */
    APPLIED,
    /** 已忽略 */
    IGNORED
}
