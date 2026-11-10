package com.lemon.erp.modules.scm.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购入库填写了运费（与入库同一事务）：财务生成运费应付单（来源 PURCHASE_FREIGHT，借 存货 / 贷 应付账款），
 * 运费已按行分摊计入存货成本（scm_trade_doc_item.freight_share）
 *
 * @param payeeId 应付对象：供应商代垫时为该入库单的供应商，本公司承担时为承运商（供应商）
 */
public record PurchaseFreightEvent(Long docId, String docNo, Long payeeId, LocalDate bizDate, BigDecimal freight, Long deptId) {
}
