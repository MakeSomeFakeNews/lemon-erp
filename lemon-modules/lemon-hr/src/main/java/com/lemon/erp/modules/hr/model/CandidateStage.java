package com.lemon.erp.modules.hr.model;

/**
 * 应聘阶段：应聘者所处阶段（字典 hr_candidate_stage）
 */
public enum CandidateStage {
    /** 新简历 */
    NEW,
    /** 面试中 */
    INTERVIEW,
    /** 已发 Offer */
    OFFER,
    /** 已入职 */
    HIRED,
    /** 已淘汰 */
    REJECTED;

    /** 是否在流程中（未录用、未淘汰），可以安排面试、发 Offer、淘汰 */
    public boolean isActive() {
        return this == NEW || this == INTERVIEW || this == OFFER;
    }
}
