package com.lemon.erp.modules.ai.model;

/** 数据整理建议的种类（ai_cleanse_suggestion.kind） */
public enum AiCleanseKind {
    /** 疑似重复：建议停用该记录，保留同组的另一条 */
    DUPLICATE,
    /** 规范化：把字段改为建议值（名称、规格写法，分类、单位等） */
    NORMALIZE,
    /** 缺失字段提示：只提示，不能采纳 */
    MISSING
}
