package com.lemon.erp.modules.hr.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 考核表状态：个人考核表的状态（字典 hr_appraisal_status）
 */
public enum AppraisalStatus {
    /** 待自评 */
    SELF,
    /** 待评分 */
    REVIEW,
    /** 已完成 */
    DONE;

    /** 评分人待评分的状态（员工未自评时也可以直接评分） */
    public static final Set<AppraisalStatus> REVIEWABLE = EnumSet.of(SELF, REVIEW);
}
