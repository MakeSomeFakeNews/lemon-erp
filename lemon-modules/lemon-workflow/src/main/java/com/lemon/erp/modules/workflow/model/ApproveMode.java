package com.lemon.erp.modules.workflow.model;

/**
 * 多人审批方式
 */
public enum ApproveMode {

    /** 依次审批：按顺序逐个审批 */
    SEQUENTIAL,
    /** 会签：所有人同时审批，全部同意才通过 */
    AND,
    /** 或签：所有人同时审批，一人同意即通过 */
    OR
}
