package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Sort;
import com.lemon.erp.framework.data.SortParam;
import com.lemon.erp.framework.data.Sorts;
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
     * @param kind    AR 应收（客户）/ AP 应付（供应商）
     * @param withAp  挂接应付（仅应收，需要应付的查看权限）：同时列出关联供应商的应付，余额 = 应收 − 应付
     * @param current 页码，与 size 都为空时返回全部明细（打印、导出）
     * @return {partnerName, opening, rows: [{bizDate, type, docNo, sourceType, sourceNo, increase, decrease, discount, balance, remark, linkedAp}],
     * increase, decrease, closing, linkedSupplierName, total, pageOpening}；合计与期末基于全部明细，每行余额基于该行之前的全部明细
     */
    public FinAnalysis.PartnerStatement statement(BillKind kind, Long partnerId, LocalDate begin, LocalDate end, boolean withAp,
                                                  Long current, Long size) {
        FinBillService.checkKind(kind);
        boolean ar = kind == BillKind.AR;
        if (!permissionService.hasPerm(ar ? "fin:ar:list" : "fin:ap:list")) {
            throw new BizException(ar ? "fin.error.noPermissionViewReceivables" : "fin.error.noPermissionViewPayables");
        }
        checkRange(begin, end);
        ScmSupplier linked = billService.linkedAp(kind, withAp) ? supplierService.linkedSupplier(partnerId) : null;
        FinAnalysis.LedgerQuery query = new FinAnalysis.LedgerQuery(kind, kind.settleKind(), partnerId, linked == null ? null : linked.getId(),
                begin, end);
        BigDecimal opening = openingOf(kind, partnerId, begin);
        if (linked != null) {
            opening = opening.subtract(openingOf(BillKind.AP, linked.getId(), begin));
        }
        FinAnalysis.LedgerTotals totals = ledgerMapper.selectFlowTotals(query);
        Page<FinAnalysis.LedgerRow> page = size == null ? null : Page.of(current == null ? 1 : current, size, totals.getCount(), false);
        List<FinAnalysis.StatementLine> rows = lines(ledgerMapper.selectFlows(page, query), opening);
        BigDecimal pageOpening = rows.isEmpty() ? opening : pageOpening(rows.get(0));
        return new FinAnalysis.PartnerStatement(billService.partnerName(kind, partnerId), opening, rows, totals.getIncrease(),
                totals.getDecrease(), opening.add(totals.getIncrease()).subtract(totals.getDecrease()),
                linked == null ? null : linked.getCode() + " " + linked.getName(), totals.getCount(), pageOpening);
    }

    /**
     * 客户对账单的快照（不挂接应付，权限由调用方检查）：期初、期间合计和全部明细
     */
    public Ledger customerLedger(Long customerId, LocalDate begin, LocalDate end) {
        checkRange(begin, end);
        FinAnalysis.LedgerQuery query = new FinAnalysis.LedgerQuery(BillKind.AR, SettleKind.RECEIPT, customerId, null, begin, end);
        BigDecimal opening = openingOf(BillKind.AR, customerId, begin);
        return new Ledger(opening, ledgerMapper.selectFlowTotals(query), lines(ledgerMapper.selectFlows(null, query), opening));
    }

    /** @param totals 期间合计，期末 = opening + increase − decrease */
    public record Ledger(BigDecimal opening, FinAnalysis.LedgerTotals totals, List<FinAnalysis.StatementLine> rows) {
    }

    private static void checkRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("fin.error.invalidDateRange");
        }
    }

    private static List<FinAnalysis.StatementLine> lines(List<FinAnalysis.LedgerRow> rows, BigDecimal opening) {
        return rows.stream().map(r -> new FinAnalysis.StatementLine(r.getBizDate(), r.getType(), r.getDocNo(), r.getSourceType(),
                r.getSourceNo(), r.getRemark(), r.getIncrease(), r.getDecrease(), r.getDiscount(), opening.add(r.getRunning()),
                r.isLinkedAp())).toList();
    }

    /** 某行之前的余额 = 该行余额 − 增加 + 减少 */
    private static BigDecimal pageOpening(FinAnalysis.StatementLine first) {
        return first.balance().subtract(Decimals.nz(first.increase())).add(Decimals.nz(first.decrease()));
    }

    /** 期初余额 = 之前的应收应付单 − 收付款（含折让）− 预收预付 − 应收应付对冲 */
    private BigDecimal openingOf(BillKind kind, Long partnerId, LocalDate begin) {
        SettleKind settleKind = kind.settleKind();
        return ledgerMapper.sumBillsBefore(kind, partnerId, begin)
                .subtract(ledgerMapper.sumSettlementsBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumAdvancesBefore(settleKind, partnerId, begin))
                .subtract(ledgerMapper.sumNettingsBefore(kind, partnerId, begin));
    }

    /**
     * 往来汇总：一个往来单位一行，期初余额、单据余额、预收预付未冲抵（负数）、坏账核销和收付款折让（单列，已从单据余额中扣除）、净欠款（当前余额，本位币）。
     * 口径与往来对账单一致：对账单截至今天的期末余额 = 净欠款 + 坏账核销（对账单不扣坏账）。
     * 应收勾选挂接应付时，另列关联供应商的应付净额（负数）并从净欠款中扣除
     */
    public PageResult<PartnerSummaryRow> summary(long current, long size, PartnerSummaryRow.Query query, SortParam sort) {
        FinBillService.checkKind(query.kind());
        boolean withAp = billService.linkedAp(query.kind(), query.withAp());
        Sort order = summarySorts(withAp).resolve(sort);
        Page<PartnerSummaryRow> page = Page.of(current, size);
        ledgerMapper.selectSummary(page, query, query.kind().settleKind(), withAp, order);
        return PageResult.of(page);
    }

    /** 往来汇总可排序的列（issue #122）：挂接应付时另有关联应付 */
    public static Sorts.Fields summarySorts(boolean withAp) {
        return Sorts.fields("partner", "opening", "billBalance", "advanceBalance", "badDebt", "discount", "net").addIf(withAp, "linkedAp");
    }

    /** 往来汇总的合计行 */
    public PartnerSummaryRow summaryTotal(PartnerSummaryRow.Query query) {
        FinBillService.checkKind(query.kind());
        return ledgerMapper.selectSummaryTotal(query, query.kind().settleKind(), billService.linkedAp(query.kind(), query.withAp()));
    }
}
