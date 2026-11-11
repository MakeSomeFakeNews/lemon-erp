package com.lemon.erp.modules.scm.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购入库填写了运费（与入库同一事务）：财务生成运费应付单（来源 PURCHASE_FREIGHT）。
 * 计入存货成本时运费已按行分摊（scm_trade_doc_item.freight_share），凭证 借 存货 / 贷 应付账款；
 * 计入当期费用时（本公司承担且业务参数 scm.purchaseFreight.cost 为 EXPENSE）不分摊，凭证 借 采购运费科目 / 贷 应付账款
 *
 * @param payeeId 应付对象：供应商代垫时为该入库单的供应商，本公司承担时为承运商（供应商）
 * @param expense 是否计入当期费用
 */
public record PurchaseFreightEvent(Long docId, String docNo, Long payeeId, LocalDate bizDate, BigDecimal freight, Long deptId,
                                   boolean expense) {
}
