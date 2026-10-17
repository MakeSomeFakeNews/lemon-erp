package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.mapper.FinPartnerLedgerMapper;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import com.lemon.erp.modules.fin.model.SettleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 往来对账单：往来单位在期间内的应收 / 应付单（增加余额）、收 / 付款和预收 / 预付款（减少余额），
 * 期初余额、逐笔余额、期末余额，可打印给客户 / 供应商核对。预收预付在收付时即冲减余额（冲抵应收应付单不再重复计入）；坏账核销不在对账单中体现
 */
@Service
@RequiredArgsConstructor
public class FinPartnerLedgerService {

    private final FinPartnerLedgerMapper ledgerMapper;
    private final FinBillService billService;
    private final PermissionService permissionService;

    /**
     * @param kind AR 应收（客户）/ AP 应付（供应商）
     * @return {partnerName, opening, rows: [{bizDate, type, docNo, sourceType, sourceNo, increase, decrease, balance, remark}], increase, decrease, closing}
     */
    public FinAnalysis.PartnerStatement statement(BillKind kind, Long partnerId, LocalDate begin, LocalDate end) {
        FinBillService.checkKind(kind);
        boolean ar = kind == BillKind.AR;
        if (!permissionService.hasPerm(ar ? "fin:ar:list" : "fin:ap:list")) {
            throw new BizException(ar ? "fin.error.noPermissionViewReceivables" : "fin.error.noPermissionViewPayables");
        }
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("fin.error.invalidDateRange");
        }
        SettleKind settleKind = kind.settleKind();
        BigDecimal opening = ledgerMapper.sumBillsBefore(kind, partnerId, begin)
                .subtract(ledgerMapper.sumSettlementsBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumAdvancesBefore(settleKind, partnerId, begin));
        BigDecimal balance = opening;
        BigDecimal increase = BigDecimal.ZERO;
        BigDecimal decrease = BigDecimal.ZERO;
        List<FinAnalysis.StatementLine> rows = new ArrayList<>();
        for (FinAnalysis.PartnerFlow r : ledgerMapper.selectRows(kind, settleKind, partnerId, begin, end)) {
            BigDecimal amount = r.getAmount();
            boolean bill = r.getType() == PartnerLedgerType.BILL;
            balance = bill ? balance.add(amount) : balance.subtract(amount);
            rows.add(new FinAnalysis.StatementLine(r.getBizDate(), r.getType(), r.getDocNo(), r.getSourceType(), r.getSourceNo(),
                    r.getRemark(), bill ? amount : null, bill ? null : amount, balance));
            if (bill) {
                increase = increase.add(amount);
            } else {
                decrease = decrease.add(amount);
            }
        }
        return new FinAnalysis.PartnerStatement(billService.partnerName(kind, partnerId), opening, rows, increase, decrease, balance);
    }
}
