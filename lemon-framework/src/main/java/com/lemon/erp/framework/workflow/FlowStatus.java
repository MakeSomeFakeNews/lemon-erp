package com.lemon.erp.framework.workflow;

/**
 * 流程实例状态，保存在 Flowable 流程实例的 businessStatus 中，与字典 wf_status 对应
 */
public enum FlowStatus {

    /** 审批中 */
    RUNNING,
    /** 审批通过 */
    APPROVED,
    /** 审批拒绝 */
    REJECTED,
    /** 发起人撤回或管理员终止 */
    CANCELED;

    /** 未知取值返回 null */
    public static FlowStatus of(String name) {
        for (FlowStatus status : values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        return null;
    }
}
