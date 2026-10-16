package com.lemon.erp.modules.workflow.model;

/**
 * 审批记录中的动作。APPROVE、REJECT、TRANSFER、CANCEL、TERMINATE 作为审批意见（Comment）的类型保存，
 * APPROVE、REJECT 同时作为任务局部变量 wfAction 保存，用于已办列表；与字典 wf_task_action 对应
 */
public enum ApprovalAction {

    /** 发起 */
    START,
    APPROVE,
    REJECT,
    TRANSFER,
    /** 发起人撤回 */
    CANCEL,
    /** 管理员终止 */
    TERMINATE,
    /** 抄送 */
    CC,
    /** 待审批 */
    PENDING;

    /** 未知取值返回 null */
    public static ApprovalAction of(String name) {
        for (ApprovalAction action : values()) {
            if (action.name().equals(name)) {
                return action;
            }
        }
        return null;
    }
}
