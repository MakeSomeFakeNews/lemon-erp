package com.lemon.erp.modules.crm.model;

/**
 * 工单处理动作：服务工单处理记录的动作
 */
public enum TicketAction {
    /** 受理 */
    CREATE,
    /** 指派 */
    ASSIGN,
    /** 处理 */
    PROCESS,
    /** 解决 */
    RESOLVE,
    /** 关闭 */
    CLOSE,
    /** 重新打开 */
    REOPEN
}
