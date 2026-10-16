package com.lemon.erp.modules.mes.model;

/**
 * 不合格品处理单状态，与字典 qms_ncr_status 对应
 */
public enum NcrStatus {
    /** 待评审 */
    OPEN,
    /** 审批中 */
    APPROVING,
    /** 已处置 */
    DONE,
    /** 已取消 */
    CANCELLED
}
