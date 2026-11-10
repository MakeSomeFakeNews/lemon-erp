package com.lemon.erp.modules.system.model;

/**
 * 通知公告的发布范围（字典 sys_notice_scope）：发布时按范围确定接收人（启用的内部用户，不含门户账号）
 */
public enum NoticeScope {
    /** 全员 */
    ALL,
    /** 指定部门（含下级部门） */
    DEPT,
    /** 指定角色 */
    ROLE
}
