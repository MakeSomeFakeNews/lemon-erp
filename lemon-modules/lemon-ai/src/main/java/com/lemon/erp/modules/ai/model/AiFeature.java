package com.lemon.erp.modules.ai.model;

/** 调用大模型的功能，用于功能开关和用量统计（ai_usage.feature） */
public enum AiFeature {
    /** AI 问数（对话、工具调用） */
    CHAT("ai.enum.feature.CHAT"),
    /** 智能录入（图片 / PDF 识别成单据草稿） */
    EXTRACT("ai.enum.feature.EXTRACT"),
    /** 数据整理（主数据查重、规范化） */
    CLEANSE("ai.enum.feature.CLEANSE"),
    /** 智能报告（周报 / 月报） */
    REPORT("ai.enum.feature.REPORT"),
    /** 「AI 设置」中的测试连接，不受功能开关限制 */
    TEST("ai.enum.feature.TEST");

    private final String labelKey;

    AiFeature(String labelKey) {
        this.labelKey = labelKey;
    }

    /** 名称的消息键 */
    public String labelKey() {
        return labelKey;
    }
}
