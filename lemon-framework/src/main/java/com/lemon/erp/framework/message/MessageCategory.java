package com.lemon.erp.framework.message;

/**
 * 消息类别：站内消息的类别（字典 sys_message_category）
 */
public enum MessageCategory {
    /** 审批 */
    APPROVAL,
    /** 提醒 */
    REMIND,
    /** 预警 */
    ALERT,
    /** 系统 */
    SYSTEM,
    /** 公告（发布通知公告时发给范围内的用户，阅读公告时一并标为已读） */
    NOTICE
}
