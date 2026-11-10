package com.lemon.erp.framework.workflow;

/**
 * 审批状态：需审批生效的业务记录（人事异动、付款、项目费用）的状态（字典 sys_approve_status）
 */
public enum ApproveStatus {
    /** 草稿（收付款开启「存草稿」时：可修改、删除，确认后才生效或提交审批） */
    DRAFT,
    /** 已生效 */
    APPROVED,
    /** 审批中 */
    APPROVING,
    /** 已拒绝 */
    REJECTED,
    /** 已撤回 */
    CANCELED,
    /** 已生效 */
    DONE
}
