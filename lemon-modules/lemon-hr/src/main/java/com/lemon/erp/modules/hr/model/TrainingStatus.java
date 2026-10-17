package com.lemon.erp.modules.hr.model;

/**
 * 培训状态：培训的状态（字典 hr_training_status）。计划中 → 进行中 → 已结束；计划中、进行中可以取消
 */
public enum TrainingStatus {
    /** 计划中 */
    PLANNED,
    /** 进行中 */
    ONGOING,
    /** 已结束 */
    FINISHED,
    /** 已取消 */
    CANCELED;

    /** 未结束、未取消：可以修改、调整学员、取消 */
    public boolean isEditable() {
        return this == PLANNED || this == ONGOING;
    }

    /** 未开始（计划中或已取消）：可以删除 */
    public boolean isDeletable() {
        return this == PLANNED || this == CANCELED;
    }

    /** 已开始（进行中或已结束）：可以录入出勤和成绩 */
    public boolean isRecordable() {
        return this == ONGOING || this == FINISHED;
    }

    /** 能否从当前状态变为 target：开始、结束、取消 */
    public boolean canChangeTo(TrainingStatus target) {
        if (target == null) {
            return false;
        }
        return switch (target) {
            case ONGOING -> this == PLANNED;
            case FINISHED -> this == ONGOING;
            case CANCELED -> isEditable();
            case PLANNED -> false;
        };
    }
}
