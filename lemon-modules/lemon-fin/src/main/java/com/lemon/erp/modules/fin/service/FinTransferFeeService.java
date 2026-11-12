package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.IoCategoryCode;
import com.lemon.erp.modules.wms.event.TransferFeeEvent;
import com.lemon.erp.modules.wms.model.TransferFeeSettle;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 调拨单的调拨费用（配送费，issue #104），处理 {@link TransferFeeEvent}，与调拨单确认 / 冲销同一事务（任一失败整单回滚）。
 * 管理方式与销售出库本公司承担的运费一致，计入当期费用、不计入存货成本：
 * <ul>
 *   <li>现付：生成费用支出单（内置类别「调拨费用」，来源为调拨单），出纳确认后 借 调拨费用 / 贷 付款账户</li>
 *   <li>月结：生成承运商应付单（来源「调拨费用」），借 调拨费用（凭证科目设置，默认销售费用）/ 贷 应付账款-承运商</li>
 * </ul>
 * 冲销调拨单时费用为负数，生成红字的费用支出单 / 应付单，回写到红字调拨单
 */
@Service
@RequiredArgsConstructor
public class FinTransferFeeService {

    private final FinBillService billService;
    private final FinIoService ioService;
    private final WmsStockDocService stockDocService;

    @EventListener
    public void onTransferFee(TransferFeeEvent e) {
        String remark = e.reversedNo() == null ? "调拨单 " + e.docNo() + " 调拨费用" // i18n-ignore 存库
                : "冲销调拨单 " + e.reversedNo() + " 调拨费用（" + e.docNo() + "）"; // i18n-ignore 存库
        Long ioId = null;
        Long billId = null;
        if (e.settle() == TransferFeeSettle.CASH) {
            ioId = ioService.createGenerated(IoCategoryCode.TRANSFER_FEE, e.bizDate(), e.accountId(), e.carrierId(), e.deptId(), e.fee(),
                    BillSource.TRANSFER_FEE, e.docId(), e.docNo(), remark);
        } else {
            billId = billService.createFreightBill(BillKind.AP, e.carrierId(), BillSource.TRANSFER_FEE, e.docId(), e.docNo(), e.bizDate(),
                    e.fee(), AccountRole.TRANSFER_FEE, e.deptId(), remark).getId();
        }
        stockDocService.bindFee(e.docId(), billId, ioId);
    }
}
