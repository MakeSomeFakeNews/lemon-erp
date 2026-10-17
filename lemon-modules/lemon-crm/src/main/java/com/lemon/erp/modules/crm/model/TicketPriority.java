package com.lemon.erp.modules.crm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单优先级：服务工单的优先级与处理时限（字典 crm_ticket_priority）
 */
@Getter
@AllArgsConstructor
public enum TicketPriority {
    /** 紧急 */
    URGENT(4),
    /** 高 */
    HIGH(24),
    /** 中 */
    MEDIUM(72),
    /** 低 */
    LOW(168);

    /** 处理时限（小时） */
    private final int slaHours;
}
