package com.lemon.erp.modules.system.model;

/**
 * 自定义字段类型（字典 sys_custom_field_type）
 */
public enum CustomFieldType {
    TEXT,
    TEXTAREA,
    /** 数字：保存为十进制文本 */
    NUMBER,
    /** 日期：yyyy-MM-dd */
    DATE,
    /** 下拉：值必须是选项之一 */
    SELECT,
    /** 是否：true / false */
    BOOL
}
