package com.lemon.erp.modules.scm.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 可承诺量（ATP）的一笔未来供需
 *
 * @param type  PO 采购在途 / WO 生产在制 / SO 销售待发 / WM 工单待领料
 * @param qty   供应为正，需求为负
 */
public record AtpLine(LocalDate date, String type, String docNo, BigDecimal qty) {
}
