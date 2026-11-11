package com.lemon.erp.modules.scm.event;

import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.FreightSettle;
import com.lemon.erp.modules.scm.model.TradeKind;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售出库、销售退货、采购退货填写了运费（与出入库同一事务，采购入库的运费见 {@link PurchaseFreightEvent}），财务据此生成：
 * <ul>
 *   <li>销售出库本公司承担：现付生成费用支出单（类别「销售运费」），月结生成承运商应付单（借 销售运费 / 贷 应付账款）</li>
 *   <li>销售出库代垫运费（客户承担）：客户的代垫运费应收单（借 应收账款 / 贷 代垫运费）；付承运商现付生成费用支出单（类别「代垫运费」），
 *       月结生成承运商应付单（借 代垫运费 / 贷 应付账款）</li>
 *   <li>销售退货客户代垫：红字应收单（借 销售运费 / 贷 应收账款）</li>
 *   <li>采购退货代垫运费：红字应付单（借 应付账款 / 贷 代垫运费）；付承运商同销售出库代垫运费</li>
 * </ul>
 * 生成的单据由财务回写到出入库单（{@code ScmTradeDocService.bindFreight}）
 *
 * @param partnerId 往来单位（客户 / 供应商）
 * @param bearer    运费承担，见 {@link FreightBearer}
 * @param settle    付给承运商的方式，销售退货为空（客户已付给承运商）
 * @param carrierId 承运商（供应商），月结时必填，现付时可为空
 * @param accountId 现付的付款账户（现金 / 银行末级科目）
 */
public record TradeFreightEvent(Long docId, TradeKind kind, String docNo, Long partnerId, LocalDate bizDate, BigDecimal freight,
                                FreightBearer bearer, FreightSettle settle, Long carrierId, Long accountId, Long deptId) {
}
