package com.lemon.erp.modules.mes.dto;

import java.math.BigDecimal;

/**
 * 可以按委外加工生成用料的采购订单明细：有默认 BOM、还没有生成委外用料、还没有收货
 * （物料属性不是委外的自制件偶尔委外加工时，在委外用料中选择这些明细生成用料）
 */
public record OutsourceCandidate(Long orderItemId, Long materialId, String materialCode, String materialName, String spec,
                                 BigDecimal qty) {
}
