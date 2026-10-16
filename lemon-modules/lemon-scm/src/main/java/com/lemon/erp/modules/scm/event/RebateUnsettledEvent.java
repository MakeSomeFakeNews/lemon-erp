package com.lemon.erp.modules.scm.event;

/**
 * 撤销返利结算（与撤销同一事务）：财务删除红字应收单（已核销、已生成凭证时抛异常，撤销回滚）
 */
public record RebateUnsettledEvent(Long rebateId, Long billId) {
}
