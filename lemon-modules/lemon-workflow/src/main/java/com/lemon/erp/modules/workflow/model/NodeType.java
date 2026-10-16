package com.lemon.erp.modules.workflow.model;

/**
 * 流程设计器的节点类型
 */
public enum NodeType {

    /** 发起人，流程的根节点 */
    START,
    /** 审批人 */
    APPROVER,
    /** 抄送人 */
    CC,
    /** 条件分支：只走第一个满足条件的分支，都不满足时走默认分支 */
    CONDITION,
    /** 并行分支：所有分支同时进行，全部完成后汇合 */
    PARALLEL
}
