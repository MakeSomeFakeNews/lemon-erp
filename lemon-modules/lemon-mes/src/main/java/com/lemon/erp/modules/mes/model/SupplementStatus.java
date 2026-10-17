package com.lemon.erp.modules.mes.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 补料状态：补料申请的状态（字典 mes_supplement_status）
 */
public enum SupplementStatus {
    /** 审批中 */
    APPROVING,
    /** 待发料 */
    APPROVED,
    /** 已发料 */
    DONE,
    /** 已拒绝 */
    REJECTED,
    /** 已撤销 */
    CANCELED;

    /** 未结束（可撤销）的状态：审批中、待发料 */
    public static final Set<SupplementStatus> ACTIVE = Collections.unmodifiableSet(EnumSet.of(APPROVING, APPROVED));

    /** 是否未结束（审批中或待发料），可以撤销 */
    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
