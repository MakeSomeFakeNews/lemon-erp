package com.lemon.erp.modules.ai.model;

/** 数据整理任务的状态（ai_cleanse_task.status） */
public enum AiCleanseTaskStatus {
    /** 进行中（同一数据类型同一时间只能有一个） */
    RUNNING,
    /** 已完成 */
    COMPLETED,
    /** 失败（已生成的建议保留） */
    FAILED,
    /** 已取消（已生成的建议保留） */
    CANCELLED
}
