package com.lemon.erp.modules.pm.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 项目状态：项目的状态（字典 pm_project_status）
 */
public enum ProjectStatus {
    /** 规划中 */
    PLANNING,
    /** 进行中 */
    ACTIVE,
    /** 已暂停 */
    SUSPENDED,
    /** 已完成 */
    FINISHED,
    /** 已关闭 */
    CLOSED,
    /** 立项审批中 */
    APPROVING;

    /** 已完成或已关闭：不能再录入工时、费用、领料，计划进度和完工进度按 100% */
    public boolean isFinished() {
        return this == FINISHED || this == CLOSED;
    }

    /** 手工流转到本状态时允许的原状态；不能手工流转到的状态返回空集合 */
    public Set<ProjectStatus> allowedFrom() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(PLANNING, SUSPENDED);
            case SUSPENDED, FINISHED -> EnumSet.of(ACTIVE);
            case CLOSED -> EnumSet.of(FINISHED);
            case PLANNING, APPROVING -> EnumSet.noneOf(ProjectStatus.class);
        };
    }
}
