package com.lemon.erp.modules.fin.model;

/**
 * 结账检查项的结果级别
 */
public enum CheckLevel {
    /** 通过 */
    OK,
    /** 提示：可以结账 */
    WARN,
    /** 错误：不能结账 */
    ERROR
}
