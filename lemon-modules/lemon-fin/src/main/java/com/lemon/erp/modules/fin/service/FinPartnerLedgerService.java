package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.PermissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.dto.PartnerSummaryRow;
import com.lemon.erp.modules.fin.mapper.FinPartnerLedgerMapper;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 往来对账单：往来单位在期间内的应收 / 应付单（增加余额）、收 / 付款、预收 / 预付款和应收应付对冲（减少余额），
 * 期初余额、逐笔余额、期末余额，可打印给客户 / 供应商核对。预收预付在收付时即冲减余额（冲抵应收应付单不再重复计入）；坏账核销不在对账单中体现。
 * <p>
 * 挂接应付（issue #95，仅客户）：同一单位既是客户又是供应商（供应商资料中设置了关联客户）时，把关联供应商的应付也列入，
 * 余额 = 应收 − 应付：应付单计入减少，付款、预付、对冲计入增加，明细标记 linkedAp
 */
@Service
@RequiredArgsConstructor
public class FinPartnerLedgerService {

    private final FinPartnerLedgerMapper ledgerMapper;
    private final FinBillService billService;
    private final PermissionService permissionService;
    private final ScmSupplierService supplierService;

    /**
     * @param kind   AR 应收（客户）/ AP 应付（供应商）
     * @param withAp 挂接应付（仅应收，需要应付的查看权限）：同时列出关联供应商的应付，余额 = 应收 − 应付
     * @return {partnerName, opening, rows: [{bizDate, type, docNo, sourceType, sourceNo, increase, decrease, balance, remark, linkedAp}],
     * increase, decrease, closing, linkedSupplierName}
     */
    public FinAnalysis.PartnerStatement statement(BillKind kind, Long partnerId, LocalDate begin, LocalDate end, boolean withAp) {
        FinBillService.checkKind(kind);
        boolean ar = kind == BillKind.AR;
        if (!permissionService.hasPerm(ar ? "fin:ar:list" : "fin:ap:list")) {
            throw new BizException(ar ? "fin.error.noPermissionViewReceivables" : "fin.error.noPermissionViewPayables");
        }
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("fin.error.invalidDateRange");
        }
        ScmSupplier linked = billService.linkedAp(kind, withAp) ? supplierService.linkedSupplier(partnerId) : null;
        BigDecimal opening = openingOf(kind, partnerId, begin);
        List<Flow> flows = new ArrayList<>(flowsOf(kind, partnerId, begin, end, false));
        if (linked != null) {
            opening = opening.subtract(openingOf(BillKind.AP, linked.getId(), begin));
            flows = merge(flows, flowsOf(BillKind.AP, linked.getId(), begin, end, true));
        }
        BigDecimal balance = opening;
        BigDecimal increase = BigDecimal.ZERO;
        BigDecimal decrease = BigDecimal.ZERO;
        List<FinAnalysis.StatementLine> rows = new ArrayList<>();
        for (Flow f : flows) {
            FinAnalysis.PartnerFlow r = f.row();
            BigDecimal amount = r.getAmount();
            // 本方的单据增加余额；挂接的应付方向相反：应付单减少，付款、预付、对冲增加
            boolean up = (r.getType() == PartnerLedgerType.BILL) != f.linked();
            balance = up ? balance.add(amount) : balance.subtract(amount);
            rows.add(new FinAnalysis.StatementLine(r.getBizDate(), r.getType(), r.getDocNo(), r.getSourceType(), r.getSourceNo(),
                    r.getRemark(), up ? amount : null, up ? null : amount, balance, f.linked()));
            if (up) {
                increase = increase.add(amount);
            } else {
                decrease = decrease.add(amount);
            }
        }
        return new FinAnalysis.PartnerStatement(billService.partnerName(kind, partnerId), opening, rows, increase, decrease, balance,
                linked == null ? null : linked.getCode() + " " + linked.getName());
    }

    /** 期初余额 = 之前的应收应付单 − 收付款（含折让）− 预收预付 − 应收应付对冲 */
    private BigDecimal openingOf(BillKind kind, Long partnerId, LocalDate begin) {
        SettleKind settleKind = kind.settleKind();
        return ledgerMapper.sumBillsBefore(kind, partnerId, begin)
                .subtract(ledgerMapper.sumSettlementsBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumAdvancesBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumNettingsBefore(kind, partnerId, begin));
    }

    private List<Flow> flowsOf(BillKind kind, Long partnerId, LocalDate begin, LocalDate end, boolean linked) {
        return ledgerMapper.selectRows(kind, kind.settleKind(), partnerId, begin, end).stream().map(r -> new Flow(r, linked)).toList();
    }

    /** 两组已按日期排序的明细按日期合并，同一天本方在前 */
    private static List<Flow> merge(List<Flow> own, List<Flow> linked) {
        List<Flow> out = new ArrayList<>(own.size() + linked.size());
        int i = 0;
        int j = 0;
        while (i < own.size() || j < linked.size()) {
            if (j >= linked.size() || i < own.size() && !own.get(i).row().getBizDate().isAfter(linked.get(j).row().getBizDate())) {
                out.add(own.get(i++));
            } else {
                out.add(linked.get(j++));
            }
        }
        return out;
    }

    private record Flow(FinAnalysis.PartnerFlow row, boolean linked) {
    }

    /**
     * 往来汇总：一个往来单位一行，期初余额、单据余额、预收预付未冲抵（负数）、坏账核销和收付款折让（单列，已从单据余额中扣除）、净欠款（当前余额，本位币）。
     * 口径与往来对账单一致：对账单截至今天的期末余额 = 净欠款 + 坏账核销（对账单不扣坏账）。
     * 应收勾选挂接应付时，另列关联供应商的应付净额（负数）并从净欠款中扣除
     */
    public PageResult<PartnerSummaryRow> summary(long current, long size, PartnerSummaryRow.Query query) {
        FinBillService.checkKind(query.kind());
        Page<PartnerSummaryRow> page = Page.of(current, size);
        ledgerMapper.selectSummary(page, query, query.kind().settleKind(), billService.linkedAp(query.kind(), query.withAp()));
        return PageResult.of(page);
    }

    /** 往来汇总的合计行 */
    public PartnerSummaryRow summaryTotal(PartnerSummaryRow.Query query) {
        FinBillService.checkKind(query.kind());
        return ledgerMapper.selectSummaryTotal(query, query.kind().settleKind(), billService.linkedAp(query.kind(), query.withAp()));
    }
}
