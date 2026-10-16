package com.lemon.erp.modules.scm.dto;

import java.math.BigDecimal;

/**
 * 补货建议：预计库存 = 可用 + 采购在途 + 在制工单 − 销售待发 − 工单待领料，不高于再订货点时建议补货
 *
 * @param supplyType   MAKE 自制 / BUY 采购 / OUTSOURCE 委外
 * @param woNet        在制工单产出 − 工单待领料
 * @param suggestQty   建议数量 = max(再订货量, 再订货点 + 安全库存 − 预计库存)，按默认供应商的最小起订量取整；不需要补货时为 0
 */
public record ReorderSuggestion(Long materialId, String code, String name, String spec, String unit, String supplyType,
                                BigDecimal safetyStock, BigDecimal reorderPoint, BigDecimal reorderQty,
                                BigDecimal available, BigDecimal poOpen, BigDecimal soOpen, BigDecimal woNet, BigDecimal projected,
                                BigDecimal suggestQty, Long supplierId, String supplierName, BigDecimal price) {
}
