package com.lemon.erp.modules.system.model;

/**
 * 定时任务的触发方式
 */
public enum JobTrigger {
    /** 按 Cron 表达式定时触发 */
    SCHEDULE,
    /** 手动立即执行 */
    MANUAL
}
