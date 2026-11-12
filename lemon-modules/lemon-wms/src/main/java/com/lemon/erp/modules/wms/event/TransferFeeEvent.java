package com.lemon.erp.modules.wms.event;

import com.lemon.erp.modules.wms.model.TransferFeeSettle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调拨单确认（或冲销）时有调拨费用（与确认同一事务），财务据此生成：现付 → 费用支出单（类别「调拨费用」，借 调拨费用 / 贷 付款账户），
 * 月结 → 承运商应付单（借 调拨费用 / 贷 应付账款）。费用计入当期费用，不计入存货成本。
 * 冲销时 fee 为负数、docId 为红字调拨单，生成红字的费用支出单 / 应付单。生成的单据由财务回写到调拨单（{@code WmsStockDocService.bindFee}）
 *
 * @param fee        调拨费用（红字单据为负数）
 * @param carrierId  承运商（供应商），月结时必填，现付时可为空
 * @param accountId  现付的付款账户（现金 / 银行末级科目）
 * @param reversedNo 冲销时为被冲销的原调拨单号，否则为空
 */
public record TransferFeeEvent(Long docId, String docNo, LocalDate bizDate, BigDecimal fee, TransferFeeSettle settle,
                               Long carrierId, Long accountId, Long deptId, String reversedNo) {
}
