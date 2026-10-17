package com.lemon.erp.modules.crm.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 工单状态：服务工单的状态（字典 crm_ticket_status）
 */
public enum TicketStatus {
    /** 待处理 */
    OPEN,
    /** 处理中 */
    PROCESSING,
    /** 已解决 */
    RESOLVED,
    /** 已关闭 */
    CLOSED;

    /** 未解决（待处理、处理中）：可以修改、指派、处理 */
    public static final Set<TicketStatus> ACTIVE = EnumSet.of(OPEN, PROCESSING);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
