package com.lemon.erp.modules.mes.model;

/**
 * 工单完工（完工入库满数量，或 完工 + 报废 达到工单数量而自动完工；同一事务中同步发布）：撤销未完成的补料申请
 */
public record WorkOrderCompletedEvent(Long woId) {
}
