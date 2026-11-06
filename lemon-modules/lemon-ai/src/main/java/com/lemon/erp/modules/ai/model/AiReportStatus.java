package com.lemon.erp.modules.ai.model;

/** 智能报告的状态 */
public enum AiReportStatus {
    /** 正在生成（模型撰写中） */
    GENERATING,
    /** 已完成 */
    DONE,
    /** 生成失败（error 中是原因） */
    FAILED
}
