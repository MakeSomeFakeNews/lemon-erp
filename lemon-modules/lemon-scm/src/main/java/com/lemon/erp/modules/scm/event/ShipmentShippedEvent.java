package com.lemon.erp.modules.scm.event;

import com.lemon.erp.modules.scm.model.FreightBearer;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发运单首次发货（与发货同一事务）：有运费时，指定了承运商（供应商）的财务生成承运商应付单，
 * 客户承担运费（代垫）时另生成客户的代垫运费应收单
 *
 * @param carrierId  承运商（scm_supplier），为空时不生成应付单
 * @param customerId 客户
 * @param bearer     运费承担方：本公司承担计入销售费用，客户承担计入代垫运费（应收）
 */
public record ShipmentShippedEvent(Long shipmentId, String shipNo, Long carrierId, BigDecimal freight, LocalDate shipDate,
                                   Long deptId, Long customerId, FreightBearer bearer) {
}
