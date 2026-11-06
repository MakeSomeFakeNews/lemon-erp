package com.lemon.erp.modules.ai.model;

/** 数据整理任务的当前阶段（ai_cleanse_task.phase） */
public enum AiCleansePhase {
    /** 读取记录、程序分组 */
    LOADING,
    /** 模型判断疑似重复 */
    DUPLICATE,
    /** 模型给出规范化建议 */
    NORMALIZE,
    /** 结束 */
    DONE
}
