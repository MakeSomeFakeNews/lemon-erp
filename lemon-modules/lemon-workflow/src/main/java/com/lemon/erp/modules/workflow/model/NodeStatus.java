package com.lemon.erp.modules.workflow.model;

/**
 * 流程详情中节点（及走过的分支）的状态
 */
public enum NodeStatus {
    /** 已完成 */
    DONE,
    /** 进行中 */
    RUNNING,
    /** 已拒绝 */
    REJECTED
}
