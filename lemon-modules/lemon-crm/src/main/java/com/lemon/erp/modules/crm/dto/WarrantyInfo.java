package com.lemon.erp.modules.crm.dto;

import java.time.LocalDate;

/**
 * 序列号的销售与保修信息
 *
 * @param docNo       销售出库单号
 * @param warrantyEnd 保修截止日期 = 出库日期 + 保修期
 */
public record WarrantyInfo(String serialNo, Long materialId, String materialCode, String materialName, String docNo,
                           LocalDate salesDate, Long customerId, String customerName, LocalDate warrantyEnd, boolean inWarranty) {
}
