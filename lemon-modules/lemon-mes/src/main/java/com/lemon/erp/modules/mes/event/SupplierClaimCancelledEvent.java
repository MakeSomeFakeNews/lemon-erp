package com.lemon.erp.modules.mes.event;

/**
 * 已确认的供应商质量索赔取消（与取消同一事务）：财务删除对应的红字应付单（已核销或已生成凭证时抛异常回滚取消）
 */
public record SupplierClaimCancelledEvent(Long claimId, String claimNo, Long billId) {
}
