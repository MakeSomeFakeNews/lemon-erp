package com.lemon.erp.modules.hr.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 招聘需求状态：招聘需求的状态（字典 hr_recruit_status）
 */
public enum RecruitStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 招聘中 */
    OPEN,
    /** 已关闭 */
    CLOSED;

    /** 占用职位编制剩余名额的状态：审批中、招聘中 */
    public static final Set<RecruitStatus> OCCUPYING = EnumSet.of(APPROVING, OPEN);
}
