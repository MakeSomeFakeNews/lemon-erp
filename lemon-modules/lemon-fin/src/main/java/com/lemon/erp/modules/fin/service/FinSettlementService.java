package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.dto.PartnerAmountRow;
import com.lemon.erp.modules.fin.dto.SettlementDiscountRow;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.model.SettleMethods;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.framework.workflow.ApproveStatus;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.entity.FinSettlementItem;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementItemMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementMapper;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 收款 / 付款：核销同一往来单位的应收 / 应付单（可同时核销红字退货单、冲账单冲抵），金额为核销合计（实收实付），不能小于 0。
 * 每张单据可另填折让 / 抹零，单据的已核销 = 核销金额 + 折让；本次收付款总额大于核销合计时，多出的部分转为预收 / 预付款。
 * 外币单据的核销金额、折让、本次总额都是原币：核销和折让按单据记账汇率折算本位币，实收实付按收付款汇率，差额计入汇兑损益；
 * 多收 / 多付转为同币种的预收预付单（收付款汇率）。
 * 生成凭证：收款 借 银行存款、收款折让 / 贷 应收账款；付款 借 应付账款 / 贷 银行存款、付款折让
 */
@Service
@RequiredArgsConstructor
public class FinSettlementService {

    /** 付款审批流程标识（只有付款单走审批） */
    public static final String PAYMENT_PROCESS_KEY = "fin_payment";
    /** 付款审批（票据背书、开票生成的付款单也按此回调，见 FinNoteService） */
    static final ApprovalFlow PAYMENT_FLOW = new ApprovalFlow(PAYMENT_PROCESS_KEY, "fin_payment:");

    private final FinSettlementMapper settlementMapper;
    private final FinSettlementItemMapper itemMapper;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;
    private final DocNoService docNoService;
    private final FinBizPeriodGuard periodGuard;
    private final com.lemon.erp.framework.finance.ExchangeRateProvider rateProvider;
    private final ApprovalSupport approvalSupport;
    private final FinAdvanceService advanceService;

    /** @param discountOnly 只查有折让的（折让记录） */
    public PageResult<FinSettlement> page(long current, long size, SettleKind kind, Long partnerId, String keyword, boolean discountOnly) {
        checkKind(kind);
        Page<FinSettlement> page = Page.of(current, size);
        settlementMapper.selectPageList(page, kind, partnerId, keyword, discountOnly);
        return PageResult.of(page);
    }

    /** 折让记录：已生效的收付款中有折让的核销明细 */
    public PageResult<SettlementDiscountRow> discounts(long current, long size, SettlementDiscountRow.Query query) {
        checkKind(query.kind());
        if (query.beginDate() != null && query.endDate() != null && query.endDate().isBefore(query.beginDate())) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        Page<SettlementDiscountRow> page = Page.of(current, size);
        itemMapper.selectDiscountPage(page, query);
        return PageResult.of(page);
    }

    /** 已生效的收款 / 付款按往来单位汇总（日期范围），金额降序 */
    public List<PartnerAmountRow> partnerSummary(SettleKind kind, LocalDate begin, LocalDate end, String keyword) {
        if (end.isBefore(begin)) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        return settlementMapper.selectPartnerSummary(kind, begin, end, keyword);
    }

    public FinSettlement detail(Long id) {
        FinSettlement s = settlementMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("fin.error.receiptPaymentDocumentNotFound");
        }
        s.setItems(itemMapper.selectBySettlement(id));
        return s;
    }

    /** @return 新单据 ID */
    @Transactional
    public Long create(SettlementRequest req) {
        // 票据结算的收付款单由票据业务生成（只能在票据管理中删除），手工收付款不能选择
        if (SettleMethods.NOTE.equals(req.method())) {
            throw new BizException("fin.error.noteSettleMethodNotManual");
        }
        accountService.requireCashAccount(req.accountId());
        return doCreate(req);
    }

    /**
     * 以票据结算（收票、背书转让、开具应付票据）：结算科目为应收票据或应付票据，其余与收付款相同（核销往来、付款审批、生成凭证）
     *
     * @param noteAccount 应收票据 / 应付票据
     * @return 新单据 ID
     */
    @Transactional
    public Long createForNote(SettlementRequest req, AccountRole noteAccount) {
        if (!accountRules.accountId(noteAccount).equals(req.accountId())) {
            throw new BizException("fin.error.invalidNoteSettlementAccount");
        }
        return doCreate(req);
    }

    /** 审批中（付款审批未结束） */
    public static boolean isApproving(FinSettlement s) {
        return s.getStatus() == ApproveStatus.APPROVING;
    }

    private Long doCreate(SettlementRequest req) {
        checkKind(req.kind());
        periodGuard.checkOpen(req.settleDate(), req.kind() == SettleKind.RECEIPT ? "fin.label.receipt" : "fin.label.payment");
        BillKind billKind = req.kind().billKind();
        // 付款发布了审批流程时先提交审批，审批通过后核销
        boolean approval = req.kind() == SettleKind.PAYMENT && approvalSupport.isAvailable(PAYMENT_FLOW);
        Set<Long> seen = new HashSet<>();
        Map<Long, FinBill> bills = billMapper.selectByIds(req.items().stream().map(SettlementItemRequest::billId).toList())
                .stream().collect(Collectors.toMap(FinBill::getId, Function.identity()));
        // 外币：同一张收付款单只能核销同一币种的单据，核销金额为原币
        Set<String> currencies = bills.values().stream().map(b -> b.getCurrency() == null ? FinConstants.BASE_CURRENCY : b.getCurrency()).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new BizException("fin.error.receiptPaymentOnlySettleDocumentsSame");
        }
        String currency = currencies.isEmpty() ? FinConstants.BASE_CURRENCY : currencies.iterator().next();
        boolean foreign = !FinConstants.BASE_CURRENCY.equals(currency);
        BigDecimal settleRate = !foreign ? BigDecimal.ONE : req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.settleDate());
        if (settleRate == null) {
            throw new BizException("fin.error.enterExchangeRateReceiptPayment", currency);
        }
        Map<Long, BigDecimal> baseAmounts = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalFc = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalDiscountFc = BigDecimal.ZERO;
        Map<Long, BigDecimal> discountBases = new HashMap<>();
        for (SettlementItemRequest item : req.items()) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || bill.getKind() != billKind || !bill.getPartnerId().equals(req.partnerId())) {
                throw new BizException(billKind == BillKind.AR ? "fin.error.settledDocumentDoesNotBelongCustomer" : "fin.error.settledDocumentDoesNotBelongSupplier");
            }
            if (!seen.add(item.billId())) {
                throw new BizException("fin.error.duplicateDocument", bill.getBillNo());
            }
            BigDecimal discount = item.discountOrZero();
            // 折让与单据同号（外币为原币）；有折让时核销金额可以为 0（整笔抹去）
            if (discount.signum() != 0 && discount.signum() != bill.getAmount().signum()) {
                throw new BizException("fin.error.settlementDiscountInvalid", bill.getBillNo());
            }
            if (item.amount().signum() != 0 ? item.amount().signum() != bill.getAmount().signum() : discount.signum() == 0) {
                throw new BizException("fin.error.settledAmountMustNonZeroHave", bill.getBillNo());
            }
            BigDecimal[] bases = ForeignAmounts.toBase(bill, item.amount(), discount);
            BigDecimal base = bases[0];
            BigDecimal discountBase = bases[1];
            baseAmounts.put(bill.getId(), base);
            discountBases.put(bill.getId(), discountBase);
            if (approval) {
                // 审批通过后才核销，这里只校验当前的未核销金额
                BigDecimal remaining = foreign ? bill.getAmountFc().subtract(bill.getSettledFc()) : bill.getAmount().subtract(bill.getSettledAmount());
                if (item.amount().add(discount).abs().compareTo(remaining.abs()) > 0) {
                    throw new BizException("fin.error.settledAmountExceedsOutstandingAmount", bill.getBillNo(), remaining.toPlainString());
                }
            } else {
                settle(bill, base, item.amount(), discountBase, discount);
            }
            total = total.add(foreign ? base : item.amount());
            totalFc = totalFc.add(item.amount());
            totalDiscount = totalDiscount.add(discountBase);
            totalDiscountFc = totalDiscountFc.add(discount);
        }
        // 红字单据（退货、冲账）冲抵后可以为 0（如冲账金额足以抵扣欠款，不需要付款）
        if (totalFc.signum() < 0) {
            throw new BizException("fin.error.receiptPaymentAmountMustGreaterAfter");
        }
        BigDecimal advance = overpaid(req, totalFc);
        FinSettlement s = new FinSettlement();
        s.setSettleNo(docNoService.next(req.kind() == SettleKind.RECEIPT ? FinDocNos.RECEIPT : FinDocNos.PAYMENT));
        s.setKind(req.kind());
        s.setPartnerId(req.partnerId());
        s.setSettleDate(req.settleDate());
        s.setAccountId(req.accountId());
        s.setMethod(req.method());
        // 外币：实收实付本位币 = 原币 × 收付款汇率，与核销的记账本位币的差额在凭证中计入汇兑损益
        s.setAmount(foreign ? Decimals.money(totalFc.multiply(settleRate)) : total);
        s.setCurrency(currency);
        s.setExchangeRate(settleRate);
        s.setAmountFc(totalFc);
        s.setDiscountAmount(totalDiscount);
        s.setDiscountFc(totalDiscountFc);
        s.setAdvanceAmount(advance);
        s.setRemark(req.remark());
        s.setStatus(approval ? ApproveStatus.APPROVING : ApproveStatus.APPROVED);
        settlementMapper.insert(s);
        itemMapper.insertBatch(req.items().stream().map(i -> {
            FinSettlementItem item = new FinSettlementItem();
            item.setSettlementId(s.getId());
            item.setBillId(i.billId());
            item.setAmount(baseAmounts.get(i.billId()));
            item.setAmountFc(i.amount());
            item.setDiscount(discountBases.get(i.billId()));
            item.setDiscountFc(i.discountOrZero());
            return item;
        }).toList());
        if (!approval) {
            toAdvance(s);
        } else {
            // 插入时已是审批中，直接发起流程；金额为实际付出的本位币（核销 + 多付转预付），不能只按核销合计，否则可以把大额付款做成预付绕过金额条件
            BigDecimal paid = Decimals.money(s.getAmount().add(advance.multiply(settleRate)));
            approvalSupport.start(PAYMENT_FLOW, s.getId(), Map.of(
                            "amount", paid.doubleValue(),
                            "settleNo", s.getSettleNo(),
                            "partnerId", s.getPartnerId()),
                    () -> {
                    },
                    instanceId -> settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getFlowInstanceId, instanceId)
                            .eq(FinSettlement::getId, s.getId())));
        }
        return s.getId();
    }

    /** 付款审批结束：通过 → 核销应付单（超过未核销金额时审批失败）；拒绝、撤销 → 已拒绝，可删除后重新录入 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        PAYMENT_FLOW.onFinished(e, this::onPaymentFinished);
    }

    private void onPaymentFinished(Long id, boolean approved) {
        FinSettlement s = settlementMapper.selectById(id);
        if (s == null || s.getStatus() != ApproveStatus.APPROVING) {
            return;
        }
        if (approved) {
            periodGuard.checkOpen(s.getSettleDate(), "fin.label.payment");
            Map<Long, FinBill> bills = new HashMap<>();
            List<FinSettlementItem> items = itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                    .eq(FinSettlementItem::getSettlementId, s.getId()));
            billMapper.selectByIds(items.stream().map(FinSettlementItem::getBillId).toList()).forEach(b -> bills.put(b.getId(), b));
            for (FinSettlementItem item : items) {
                FinBill bill = bills.get(item.getBillId());
                if (bill == null) {
                    throw new BizException("fin.error.settledDocumentNotFound", item.getBillId());
                }
                settle(bill, item.getAmount(), item.getAmountFc(), Decimals.nz(item.getDiscount()), discountFc(item));
            }
            toAdvance(s);
        }
        StatusTransitions.tryChange(settlementMapper, FinSettlement::getId, s.getId(), FinSettlement::getStatus,
                ApproveStatus.APPROVING, approved ? ApproveStatus.APPROVED : ApproveStatus.REJECTED);
    }

    /**
     * 本次收付款总额超过核销合计的部分（多收 / 多付），须选择转为预收 / 预付；总额小于核销合计时报错。外币为原币
     *
     * @return 转预收 / 预付的金额（原币），没有为 0
     */
    private static BigDecimal overpaid(SettlementRequest req, BigDecimal settled) {
        if (req.totalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal extra = req.totalAmount().subtract(settled);
        if (extra.signum() < 0) {
            throw new BizException("fin.error.settlementTotalLessThanSettled", req.totalAmount().toPlainString(), settled.toPlainString());
        }
        if (extra.signum() > 0 && !Boolean.TRUE.equals(req.toAdvance())) {
            throw new BizException("fin.error.overpaidChooseAdvance", extra.toPlainString());
        }
        return extra;
    }

    /** 多收 / 多付的部分生成预收 / 预付单（同一事务，复用预收预付登记：立即生成凭证 借 银行 / 贷 预收账款）；外币按收付款的币种、汇率 */
    private void toAdvance(FinSettlement s) {
        if (Decimals.nz(s.getAdvanceAmount()).signum() <= 0) {
            return;
        }
        String remark = Messages.get(Messages.DEFAULT_LOCALE, "fin.text.overpaidAdvanceRemark", s.getSettleNo());
        Long advanceId = advanceService.create(new AdvanceRequest(s.getKind(), s.getPartnerId(), s.getSettleDate(), s.getAccountId(),
                s.getMethod(), s.getAdvanceAmount(), remark, s.getCurrency(), s.getExchangeRate()));
        settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getAdvanceId, advanceId)
                .eq(FinSettlement::getId, s.getId()));
        s.setAdvanceId(advanceId);
    }

    /** 核销：本位币按单据记账汇率（原币核销完时取剩余本位币，避免尾差），同时累加原币已核销；折让一并计入已核销 */
    private void settle(FinBill bill, BigDecimal base, BigDecimal fc, BigDecimal discount, BigDecimal discountFc) {
        if (billMapper.addSettled(bill.getId(), base.add(discount)) == 0) {
            FinBill latest = billMapper.selectById(bill.getId());
            BigDecimal remaining = latest == null ? BigDecimal.ZERO : latest.getAmount().subtract(latest.getSettledAmount());
            throw new BizException("fin.error.settledAmountExceedsOutstandingAmount", bill.getBillNo(), remaining.toPlainString());
        }
        billMapper.addSettledFc(bill.getId(), (fc == null ? base : fc).add(discountFc));
        if (discount.signum() != 0) {
            billMapper.addDiscount(bill.getId(), discount);
        }
    }

    /** 明细的折让原币金额（升级前的明细没有原币，与本位币相同） */
    private static BigDecimal discountFc(FinSettlementItem item) {
        BigDecimal discount = Decimals.nz(item.getDiscount());
        return item.getDiscountFc() == null || (item.getDiscountFc().signum() == 0 && discount.signum() != 0) ? discount : item.getDiscountFc();
    }

    /** 删除（未生成凭证的），恢复单据的核销金额；票据结算的收付款单须在票据管理中删除 */
    @Transactional
    public void delete(List<Long> ids) {
        doDelete(ids, false);
    }

    /** 删除票据登记、背书生成的收付款单（票据管理删除票据时） */
    @Transactional
    public void deleteForNote(List<Long> ids) {
        doDelete(ids, true);
    }

    private void doDelete(List<Long> ids, boolean forNote) {
        for (Long id : ids) {
            FinSettlement s = settlementMapper.selectById(id);
            if (s == null) {
                continue;
            }
            if (!forNote && SettleMethods.NOTE.equals(s.getMethod())) {
                throw new BizException("fin.error.generatedNoteTransactionDeleteNoteNotes", s.getSettleNo());
            }
            if (s.getVoucherId() != null) {
                throw new BizException("fin.error.alreadyHasVoucherDeleteVoucherFirst", s.getSettleNo());
            }
            if (s.getStatus() == ApproveStatus.APPROVING) {
                throw new BizException("fin.error.underApprovalWithdrawMyRequestsFirst", s.getSettleNo());
            }
            periodGuard.checkOpen(s.getSettleDate(), "fin.label.receiptPayment");
            // 多收转的预收预付一并删除（已冲抵或凭证已审核时不能删除）
            if (s.getAdvanceId() != null) {
                advanceService.delete(s.getAdvanceId());
            }
            // 已拒绝的付款没有核销过，不需要恢复
            if (s.getStatus() == ApproveStatus.APPROVED) {
                for (FinSettlementItem item : itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                        .eq(FinSettlementItem::getSettlementId, id))) {
                    BigDecimal discount = Decimals.nz(item.getDiscount());
                    billMapper.addSettled(item.getBillId(), item.getAmount().add(discount).negate());
                    billMapper.addSettledFc(item.getBillId(), (item.getAmountFc() == null ? item.getAmount() : item.getAmountFc()).add(discountFc(item)).negate());
                    if (discount.signum() != 0) {
                        billMapper.addDiscount(item.getBillId(), discount.negate());
                    }
                }
            }
            itemMapper.delete(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getSettlementId, id));
            settlementMapper.deleteById(id);
        }
    }

    /** @return 生成的凭证数 */
    @Transactional
    public int generateVouchers(List<Long> ids) {
        int count = 0;
        for (FinSettlement s : settlementMapper.selectByIds(ids)) {
            // 审批中、已拒绝的付款不生成凭证
            if (s.getVoucherId() != null || s.getStatus() != ApproveStatus.APPROVED) {
                continue;
            }
            boolean receipt = s.getKind() == SettleKind.RECEIPT;
            String summary = (receipt ? "收到 " : "支付 ") + billService.partnerName(s.getKind(), s.getPartnerId()) + " 货款"; // i18n-ignore
            Long partnerAccount = accountRules.accountId(receipt ? AccountRole.RECEIVABLE : AccountRole.PAYABLE);
            // 往来科目按核销的记账本位币，银行按实收实付本位币，外币差额计入财务费用（汇兑损益）
            List<FinSettlementItem> items = itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                    .eq(FinSettlementItem::getSettlementId, s.getId()));
            BigDecimal booked = Decimals.sum(items, FinSettlementItem::getAmount);
            BigDecimal discount = Decimals.nz(s.getDiscountAmount());
            List<VoucherLine> lines = new java.util.ArrayList<>();
            if (s.getAmount().signum() == 0 && discount.signum() == 0 && booked.signum() == 0) {
                // 红字单据（冲账、退货）恰好抵消欠款：往来科目按蓝字、红字分两行冲抵，没有资金收付
                BigDecimal blue = Decimals.sum(items.stream().filter(i -> i.getAmount().signum() > 0).toList(), FinSettlementItem::getAmount);
                lines.add(partnerLine(receipt, summary, partnerAccount, s.getPartnerId(), blue));
                lines.add(partnerLine(receipt, summary, partnerAccount, s.getPartnerId(), blue.negate()));
            } else if (receipt) {
                lines.add(VoucherLine.debit(summary, s.getAccountId(), null, s.getAmount()));
                // 收款折让：借 收款折让科目（默认财务费用）
                lines.add(VoucherLine.debit(summary + " 折让", accountRules.accountId(AccountRole.RECEIPT_DISCOUNT), null, discount)); // i18n-ignore
                lines.add(partnerLine(true, summary, partnerAccount, s.getPartnerId(), booked.add(discount)));
            } else {
                lines.add(partnerLine(false, summary, partnerAccount, s.getPartnerId(), booked.add(discount)));
                lines.add(VoucherLine.credit(summary, s.getAccountId(), null, s.getAmount()));
                // 付款折让：贷 付款折让科目（默认财务费用）
                lines.add(VoucherLine.credit(summary + " 折让", accountRules.accountId(AccountRole.PAYMENT_DISCOUNT), null, discount)); // i18n-ignore
            }
            BigDecimal diff = receipt ? s.getAmount().subtract(booked) : booked.subtract(s.getAmount());
            if (diff.signum() != 0) {
                // 收款多收 / 付款少付为汇兑收益，冲减财务费用
                lines.add(VoucherLine.credit(summary + " 汇兑损益", // i18n-ignore
                        accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, diff));
            }
            Long voucherId = voucherService.createGenerated(s.getSettleDate(), receipt ? VoucherSource.RECEIPT : VoucherSource.PAYMENT,
                    s.getSettleNo(), lines);
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getVoucherId, voucherId).eq(FinSettlement::getId, s.getId()));
            count++;
        }
        return count;
    }

    /** 往来科目分录：收款贷应收账款（客户），付款借应付账款（供应商）；金额为负数时记到另一方 */
    private static VoucherLine partnerLine(boolean receipt, String summary, Long account, Long partnerId, BigDecimal amount) {
        return receipt ? VoucherLine.credit(summary, account, null, amount).withCustomer(partnerId)
                : VoucherLine.debit(summary, account, null, amount).withSupplier(partnerId);
    }

    private static void checkKind(SettleKind kind) {
        if (kind == null) {
            throw new BizException("fin.error.invalidReceiptPaymentKind", kind);
        }
    }
}
