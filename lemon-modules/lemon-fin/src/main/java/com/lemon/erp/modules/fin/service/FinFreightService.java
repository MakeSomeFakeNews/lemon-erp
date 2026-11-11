package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.IoCategoryCode;
import com.lemon.erp.modules.scm.event.TradeFreightEvent;
import com.lemon.erp.modules.scm.model.FreightBearer;
import com.lemon.erp.modules.scm.model.FreightSettle;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 销售出库、销售退货、采购退货的运费（issue #72、#73），处理 {@link TradeFreightEvent}，与出入库同一事务（任一失败整单回滚）。
 * 运费只记一次，不是销售收入、不影响毛利和存货成本：
 * <ul>
 *   <li>销售出库本公司承担：付承运商现付生成费用支出单（类别「销售运费」）借 销售运费 / 贷 银行，月结生成承运商应付 借 销售运费 / 贷 应付账款</li>
 *   <li>销售出库代垫运费（客户承担）：客户的代垫运费应收 借 应收账款 / 贷 代垫运费；付承运商现付生成费用支出单（类别「代垫运费」）
 *       借 代垫运费 / 贷 银行，月结生成承运商应付 借 代垫运费 / 贷 应付账款</li>
 *   <li>销售退货客户代垫（本公司承担，客户已付给承运商）：红字应收 借 销售运费 / 贷 应收账款</li>
 *   <li>采购退货代垫运费（供应商承担）：红字应付 借 应付账款 / 贷 代垫运费；付承运商同销售出库代垫运费</li>
 * </ul>
 * 代垫运费科目（{@link AccountRole#FREIGHT_ADVANCE}）由往来单位的单据与付承运商的单据冲平。往来单位的运费单据单独一张（与发运单的代垫运费一致），
 * 收付款时与货款单据一起勾选，欠款合计 = 货款 ± 代垫运费。生成的单据回写到出入库单（联查）；费用支出单记录来源单据，出纳确认后生成凭证
 */
@Service
@RequiredArgsConstructor
public class FinFreightService {

    private final FinBillService billService;
    private final FinIoService ioService;
    private final ScmTradeDocService tradeDocService;

    @EventListener
    public void onTradeFreight(TradeFreightEvent e) {
        boolean company = e.bearer() == FreightBearer.COMPANY;
        BillSource source = switch (e.kind()) {
            case SALES_OUT -> BillSource.SALES_FREIGHT;
            case SALES_RETURN -> BillSource.SALES_RETURN_FREIGHT;
            case PURCHASE_RETURN -> BillSource.PURCHASE_RETURN_FREIGHT;
            case PURCHASE_IN -> throw new IllegalArgumentException("purchase freight uses PurchaseFreightEvent");
        };
        FinBill partnerBill = null;
        if (e.kind() == TradeKind.SALES_OUT && !company) {
            // 代垫运费（客户承担）：客户的代垫运费应收单
            partnerBill = billService.createFreightBill(BillKind.AR, e.partnerId(), source, e.docId(), e.docNo(), e.bizDate(), e.freight(),
                    AccountRole.FREIGHT_ADVANCE, e.deptId(), "销售出库 " + e.docNo() + " 代垫运费"); // i18n-ignore
        } else if (e.kind() == TradeKind.SALES_RETURN) {
            // 客户代垫的退货运费（本公司承担）：红字应收冲减客户欠款
            partnerBill = billService.createFreightBill(BillKind.AR, e.partnerId(), source, e.docId(), e.docNo(), e.bizDate(),
                    e.freight().negate(), AccountRole.SALES_FREIGHT, e.deptId(), "销售退货 " + e.docNo() + " 客户代垫运费"); // i18n-ignore
        } else if (e.kind() == TradeKind.PURCHASE_RETURN) {
            // 本公司替供应商垫付的退货运费：红字应付冲减欠供应商的款
            partnerBill = billService.createFreightBill(BillKind.AP, e.partnerId(), source, e.docId(), e.docNo(), e.bizDate(),
                    e.freight().negate(), AccountRole.FREIGHT_ADVANCE, e.deptId(), "采购退货 " + e.docNo() + " 代垫运费"); // i18n-ignore
        }

        Long carrierBillId = null;
        Long ioId = null;
        if (e.settle() != null) {
            // 付给承运商：本公司承担的计入销售运费，代垫的计入代垫运费（由往来单位的单据冲平）
            String remark = (e.kind() == TradeKind.SALES_OUT ? "销售出库 " : "采购退货 ") + e.docNo() + (company ? " 运费" : " 代垫运费"); // i18n-ignore
            if (e.settle() == FreightSettle.CASH) {
                ioId = ioService.createGenerated(company ? IoCategoryCode.SALES_FREIGHT : IoCategoryCode.FREIGHT_ADVANCE, e.bizDate(),
                        e.accountId(), e.carrierId(), e.deptId(), e.freight(), BillSource.valueOf(e.kind().name()), e.docId(), e.docNo(), remark);
            } else {
                carrierBillId = billService.createFreightBill(BillKind.AP, e.carrierId(), source, e.docId(), e.docNo(), e.bizDate(), e.freight(),
                        company ? AccountRole.SALES_FREIGHT : AccountRole.FREIGHT_ADVANCE, e.deptId(), remark).getId();
            }
        }
        tradeDocService.bindFreight(e.docId(), carrierBillId, partnerBill == null ? null : partnerBill.getId(), ioId);
    }
}
