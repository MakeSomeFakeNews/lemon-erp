package com.lemon.erp.modules.system.model;

/**
 * 定时任务一次执行的结果
 */
public enum JobRunStatus {
    /** 成功 */
    SUCCESS,
    /** 失败（任一租户执行失败也记为失败） */
    FAILED
}
