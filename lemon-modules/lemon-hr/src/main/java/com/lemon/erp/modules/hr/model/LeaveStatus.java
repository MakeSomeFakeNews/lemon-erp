package com.lemon.erp.modules.hr.model;

import com.lemon.erp.framework.workflow.FlowStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * 假勤状态：请假、加班申请的状态（字典 hr_leave_status）
 */
public enum LeaveStatus {
    /** 审批中 */
    APPROVING,
    /** 已通过 */
    APPROVED,
    /** 已拒绝 */
    REJECTED,
    /** 已撤销 */
    CANCELED;

    /** 有效的申请（审批中、已通过）：占用余额、显示在考勤日历中，可以撤销 */
    public static final Set<LeaveStatus> EFFECTIVE = EnumSet.of(APPROVING, APPROVED);

    /** 审批流程结束后的状态：通过、拒绝，其他（撤销、终止）为已撤销 */
    public static LeaveStatus ofFlow(FlowStatus status) {
        return switch (status) {
            case APPROVED -> APPROVED;
            case REJECTED -> REJECTED;
            default -> CANCELED;
        };
    }
}
