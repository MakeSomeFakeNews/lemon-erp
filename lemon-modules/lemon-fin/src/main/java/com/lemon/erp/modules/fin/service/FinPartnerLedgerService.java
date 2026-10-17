package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.fin.mapper.FinPartnerLedgerMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import com.lemon.erp.modules.fin.model.SettleKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Object> statement(String kind, Long partnerId, LocalDate begin, LocalDate end) {
        FinBillService.checkKind(kind);
        boolean ar = FinConstants.BILL_AR.equals(kind);
        if (!permissionService.hasPerm(ar ? "fin:ar:list" : "fin:ap:list")) {
            throw new BizException("没有权限查看" + (ar ? "应收" : "应付"));
        }
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("日期范围不正确");
        }
        String settleKind = (ar ? SettleKind.RECEIPT : SettleKind.PAYMENT).name();
        BigDecimal opening = ledgerMapper.sumBillsBefore(kind, partnerId, begin)
                .subtract(ledgerMapper.sumSettlementsBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumAdvancesBefore(settleKind, partnerId, begin));
        BigDecimal balance = opening;
        BigDecimal increase = BigDecimal.ZERO;
        BigDecimal decrease = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : ledgerMapper.selectRows(kind, settleKind, partnerId, begin, end)) {
            BigDecimal amount = new BigDecimal(r.get("amount").toString());
            boolean bill = PartnerLedgerType.BILL.name().equals(r.get("type"));
            Map<String, Object> row = new LinkedHashMap<>(r);
            row.remove("amount");
            row.put("increase", bill ? amount : null);
            row.put("decrease", bill ? null : amount);
            balance = bill ? balance.add(amount) : balance.subtract(amount);
            row.put("balance", balance);
            if (bill) {
                increase = increase.add(amount);
            } else {
                decrease = decrease.add(amount);
            }
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partnerName", billService.partnerName(kind, partnerId));
        result.put("opening", opening);
        result.put("rows", rows);
        result.put("increase", increase);
        result.put("decrease", decrease);
        result.put("closing", balance);
        return result;
    }
}
