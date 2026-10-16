package com.lemon.erp.modules.mes.model;

/**
 * 工单关闭（同一事务中同步发布）：撤销未完成的补料申请
 */
public record WorkOrderClosedEvent(Long woId) {
}
