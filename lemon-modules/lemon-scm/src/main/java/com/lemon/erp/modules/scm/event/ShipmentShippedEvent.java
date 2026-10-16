package com.lemon.erp.modules.scm.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发运单首次发货（与发货同一事务）：有运费且指定了承运商（供应商）时，财务生成承运商应付单
 *
 * @param carrierId 承运商（scm_supplier）
 */
public record ShipmentShippedEvent(Long shipmentId, String shipNo, Long carrierId, BigDecimal freight, LocalDate shipDate,
                                   Long deptId) {
}
