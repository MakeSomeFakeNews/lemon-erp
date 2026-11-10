package com.lemon.erp.modules.system.model;

/**
 * 通知公告状态（字典 sys_notice_status）：草稿 → 已发布 → 已撤回，撤回后可以修改、再次发布
 */
public enum NoticeStatus {
    /** 草稿 */
    DRAFT,
    /** 已发布 */
    PUBLISHED,
    /** 已撤回 */
    WITHDRAWN
}
