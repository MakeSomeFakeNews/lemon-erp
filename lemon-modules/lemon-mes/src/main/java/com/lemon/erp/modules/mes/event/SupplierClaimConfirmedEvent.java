package com.lemon.erp.modules.mes.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商质量索赔确认（与确认同一事务）：财务生成红字应付单冲减应付，并回写索赔单的 billId
 *
 * @param amount 索赔金额（正数）
 */
public record SupplierClaimConfirmedEvent(Long claimId, String claimNo, Long supplierId, BigDecimal amount, LocalDate claimDate,
                                          String reason) {
}
