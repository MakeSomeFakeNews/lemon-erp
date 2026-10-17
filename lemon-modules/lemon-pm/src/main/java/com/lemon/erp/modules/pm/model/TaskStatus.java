package com.lemon.erp.modules.pm.model;

/**
 * 任务状态：项目任务的状态（字典 pm_task_status）
 */
public enum TaskStatus {
    /** 未开始 */
    TODO,
    /** 进行中 */
    DOING,
    /** 已完成 */
    DONE;

    /** 完成进度（%）为 100 时的状态 */
    public static final int FULL_PROGRESS = 100;

    /** 按末级任务进度取状态：0 未开始、100 已完成、其余进行中 */
    public static TaskStatus ofProgress(int progress) {
        return progress == 0 ? TODO : progress == FULL_PROGRESS ? DONE : DOING;
    }
}
