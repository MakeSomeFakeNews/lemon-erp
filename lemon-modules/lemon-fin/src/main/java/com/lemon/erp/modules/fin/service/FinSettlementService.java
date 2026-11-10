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
import com.lemon.erp.modules.fin.mapper.FinAdvanceApplyMapper;
import com.lemon.erp.modules.fin.mapper.FinAdvanceMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementAdvanceMapper;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinAdvanceApply;
import com.lemon.erp.modules.fin.entity.FinSettlementAdvance;
import com.lemon.erp.modules.fin.dto.SettlementAdvanceRequest;
import com.lemon.erp.framework.rule.BizRules;
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
 * 多收 / 多付转为同币种的预收预付单（收付款汇率）。也可以勾选该单位未冲抵的预收 / 预付款一起冲抵，本次实收实付 = 核销合计 − 冲抵合计。
 * 业务参数「收付款先存草稿」开启时先保存为草稿（可修改、删除），确认后才生效；付款发布了审批流程时确认即提交审批，审批通过后生效。
 * 生成凭证：收款 借 银行存款、收款折让、预收账款（冲抵）/ 贷 应收账款；付款 借 应付账款 / 贷 银行存款、付款折让、预付账款（冲抵）
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
    private final FinAdvanceMapper advanceMapper;
    private final FinAdvanceApplyMapper applyMapper;
    private final FinSettlementAdvanceMapper settlementAdvanceMapper;
    private final BizRules bizRules;

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
        s.setAdvances(settlementAdvanceMapper.selectBySettlement(id));
        return s;
    }

    /**
     * 新增收付款：业务参数「收付款先存草稿」关闭时保存即生效（核销、冲抵预收预付、多收转预收，付款发布了审批流程时提交审批）；
     * 开启时保存为草稿（可修改、删除），{@code req.confirm()} 为 true 时保存并确认
     *
     * @return 新单据 ID
     */
    @Transactional
    public Long create(SettlementRequest req) {
        // 票据结算的收付款单由票据业务生成（只能在票据管理中删除），手工收付款不能选择
        if (SettleMethods.NOTE.equals(req.method())) {
            throw new BizException("fin.error.noteSettleMethodNotManual");
        }
        accountService.requireCashAccount(req.accountId());
        FinSettlement s = save(null, req);
        if (confirmOnSave(req)) {
            doConfirm(s);
        }
        return s.getId();
    }

    /**
     * 以票据结算（收票、背书转让、开具应付票据）：结算科目为应收票据或应付票据，其余与收付款相同（核销往来、付款审批、生成凭证），
     * 不存草稿
     *
     * @param noteAccount 应收票据 / 应付票据
     * @return 新单据 ID
     */
    @Transactional
    public Long createForNote(SettlementRequest req, AccountRole noteAccount) {
        if (!accountRules.accountId(noteAccount).equals(req.accountId())) {
            throw new BizException("fin.error.invalidNoteSettlementAccount");
        }
        FinSettlement s = save(null, req);
        doConfirm(s);
        return s.getId();
    }

    /**
     * 修改草稿或已拒绝的付款单（重新录入核销明细、冲抵的预收预付），保存后为草稿；业务参数「收付款先存草稿」关闭时
     * 或 {@code req.confirm()} 为 true 时保存并确认（已拒绝的付款再次提交审批）
     */
    @Transactional
    public void update(Long id, SettlementRequest req) {
        FinSettlement old = settlementMapper.selectById(id);
        if (old == null || old.getKind() != req.kind()) {
            throw new BizException("fin.error.receiptPaymentDocumentNotFound");
        }
        if (old.getStatus() != ApproveStatus.DRAFT && old.getStatus() != ApproveStatus.REJECTED) {
            throw new BizException("fin.error.settlementNotEditable", old.getSettleNo());
        }
        if (SettleMethods.NOTE.equals(old.getMethod()) || SettleMethods.NOTE.equals(req.method())) {
            throw new BizException("fin.error.noteSettleMethodNotManual");
        }
        accountService.requireCashAccount(req.accountId());
        periodGuard.checkOpen(old.getSettleDate(), "fin.label.receiptPayment");
        FinSettlement s = save(old, req);
        if (confirmOnSave(req)) {
            doConfirm(s);
        }
    }

    /** 确认草稿：核销单据、冲抵预收预付、多收转预收预付；付款发布了审批流程时提交审批（审批通过后才核销） */
    @Transactional
    public void confirm(Long id) {
        FinSettlement s = settlementMapper.selectById(id);
        if (s == null) {
            throw new BizException("fin.error.receiptPaymentDocumentNotFound");
        }
        if (s.getStatus() != ApproveStatus.DRAFT) {
            throw new BizException("fin.error.settlementNotDraft", s.getSettleNo());
        }
        doConfirm(s);
    }

    /** 是否启用了「收付款先存草稿」（当前租户的业务参数） */
    public boolean draftEnabled() {
        return bizRules.bool(FinBizRules.SETTLEMENT_DRAFT);
    }

    /** 保存后是否立即确认：未启用草稿时保存即生效；启用时由请求的 confirm 决定 */
    private boolean confirmOnSave(SettlementRequest req) {
        return !draftEnabled() || Boolean.TRUE.equals(req.confirm());
    }

    /** 往来单位可以冲抵的预收 / 预付款（未冲抵完的，按收付日期从早到晚） */
    public List<FinAdvance> openAdvances(SettleKind kind, Long partnerId) {
        checkKind(kind);
        return advanceService.open(kind, partnerId);
    }

    /** 审批中（付款审批未结束） */
    public static boolean isApproving(FinSettlement s) {
        return s.getStatus() == ApproveStatus.APPROVING;
    }

    /**
     * 校验并保存为草稿（新增或替换已有草稿的明细），不核销：核销金额、折让不超过单据当前的未核销金额；冲抵的预收预付
     * 按单据日期分配到蓝字单据（记在明细的 advance_offset，核销金额只记实收实付的部分）
     *
     * @param old 修改时的原单据，新增为 null
     */
    private FinSettlement save(FinSettlement old, SettlementRequest req) {
        checkKind(req.kind());
        periodGuard.checkOpen(req.settleDate(), req.kind() == SettleKind.RECEIPT ? "fin.label.receipt" : "fin.label.payment");
        BillKind billKind = req.kind().billKind();
        Set<Long> seen = new HashSet<>();
        Map<Long, FinBill> bills = billMapper.selectByIds(req.items().stream().map(SettlementItemRequest::billId).toList())
                .stream().collect(Collectors.toMap(FinBill::getId, Function.identity()));
        // 外币：同一张收付款单只能核销同一币种的单据，核销金额为原币
        Set<String> currencies = bills.values().stream().map(FinSettlementService::currencyOf).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new BizException("fin.error.receiptPaymentOnlySettleDocumentsSame");
        }
        String currency = currencies.isEmpty() ? FinConstants.BASE_CURRENCY : currencies.iterator().next();
        boolean foreign = !FinConstants.BASE_CURRENCY.equals(currency);
        BigDecimal settleRate = !foreign ? BigDecimal.ONE : req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.settleDate());
        if (settleRate == null) {
            throw new BizException("fin.error.enterExchangeRateReceiptPayment", currency);
        }
        BigDecimal totalFc = BigDecimal.ZERO;
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
            // 草稿、审批中不核销，这里校验当前的未核销金额（生效时按最新余额再次校验）
            BigDecimal remaining = foreign ? bill.getAmountFc().subtract(bill.getSettledFc()) : bill.getAmount().subtract(bill.getSettledAmount());
            if (item.amount().add(discount).abs().compareTo(remaining.abs()) > 0) {
                throw new BizException("fin.error.settledAmountExceedsOutstandingAmount", bill.getBillNo(), remaining.toPlainString());
            }
            totalFc = totalFc.add(item.amount());
        }
        // 红字单据（退货、冲账）冲抵后可以为 0（如冲账金额足以抵扣欠款，不需要付款）
        if (totalFc.signum() < 0) {
            throw new BizException("fin.error.receiptPaymentAmountMustGreaterAfter");
        }
        // 冲抵的预收预付：同一往来单位、同币种，不超过未冲抵余额；冲抵后的实收实付不能为负数
        BigDecimal offsetTotal = checkAdvances(req, currency);
        BigDecimal cashFc = totalFc.subtract(offsetTotal);
        if (cashFc.signum() < 0) {
            throw new BizException("fin.error.advanceOffsetExceedsSettled", offsetTotal.toPlainString(), totalFc.toPlainString());
        }
        BigDecimal advance = overpaid(req, cashFc);
        if (advance.signum() > 0 && offsetTotal.signum() > 0) {
            throw new BizException("fin.error.advanceOffsetWithOverpaid");
        }
        Map<Long, BigDecimal> offsets = allocateOffsets(req.items(), bills, offsetTotal);
        List<FinSettlementItem> items = new java.util.ArrayList<>();
        BigDecimal cashBase = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalDiscountFc = BigDecimal.ZERO;
        for (SettlementItemRequest i : req.items()) {
            FinBill bill = bills.get(i.billId());
            BigDecimal offset = offsets.getOrDefault(i.billId(), BigDecimal.ZERO);
            BigDecimal cash = i.amount().subtract(offset);
            // 本位币为当前余额下的折算（草稿生效时按最新余额重新折算）
            BigDecimal[] bases = ForeignAmounts.toBase(bill, cash, i.discountOrZero());
            FinSettlementItem item = new FinSettlementItem();
            item.setBillId(i.billId());
            item.setAmount(bases[0]);
            item.setAmountFc(cash);
            item.setDiscount(bases[1]);
            item.setDiscountFc(i.discountOrZero());
            item.setAdvanceOffset(offset);
            items.add(item);
            cashBase = cashBase.add(bases[0]);
            totalDiscount = totalDiscount.add(bases[1]);
            totalDiscountFc = totalDiscountFc.add(i.discountOrZero());
        }
        FinSettlement s = old != null ? old : new FinSettlement();
        if (old == null) {
            s.setSettleNo(docNoService.next(req.kind() == SettleKind.RECEIPT ? FinDocNos.RECEIPT : FinDocNos.PAYMENT));
            s.setKind(req.kind());
        }
        s.setPartnerId(req.partnerId());
        s.setSettleDate(req.settleDate());
        s.setAccountId(req.accountId());
        s.setMethod(req.method());
        // 外币：实收实付本位币 = 原币 × 收付款汇率，与核销的记账本位币的差额在凭证中计入汇兑损益
        s.setAmount(foreign ? Decimals.money(cashFc.multiply(settleRate)) : cashBase);
        s.setCurrency(currency);
        s.setExchangeRate(settleRate);
        s.setAmountFc(cashFc);
        s.setDiscountAmount(totalDiscount);
        s.setDiscountFc(totalDiscountFc);
        s.setAdvanceAmount(advance);
        s.setAdvanceOffset(offsetTotal);
        s.setRemark(req.remark());
        s.setStatus(ApproveStatus.DRAFT);
        if (old == null) {
            settlementMapper.insert(s);
        } else {
            // 已拒绝的付款再次编辑：清除原流程实例，确认时重新发起
            s.setFlowInstanceId(null);
            settlementMapper.updateById(s);
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getFlowInstanceId, null)
                    .eq(FinSettlement::getId, s.getId()));
            itemMapper.delete(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getSettlementId, s.getId()));
            settlementAdvanceMapper.delete(Wrappers.<FinSettlementAdvance>lambdaQuery().eq(FinSettlementAdvance::getSettlementId, s.getId()));
        }
        items.forEach(i -> i.setSettlementId(s.getId()));
        itemMapper.insertBatch(items);
        if (!req.advanceList().isEmpty()) {
            settlementAdvanceMapper.insertBatch(req.advanceList().stream().map(a -> {
                FinSettlementAdvance sa = new FinSettlementAdvance();
                sa.setSettlementId(s.getId());
                sa.setAdvanceId(a.advanceId());
                sa.setAmount(a.amount());
                return sa;
            }).toList());
        }
        return s;
    }

    /**
     * 校验冲抵的预收预付：属于该往来单位、与核销单据同币种、金额不超过未冲抵余额（原币），不能重复选择
     *
     * @return 冲抵合计（原币）
     */
    private BigDecimal checkAdvances(SettlementRequest req, String currency) {
        List<SettlementAdvanceRequest> list = req.advanceList();
        if (list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, FinAdvance> advances = advanceMapper.selectByIds(list.stream().map(SettlementAdvanceRequest::advanceId).toList())
                .stream().collect(Collectors.toMap(FinAdvance::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementAdvanceRequest r : list) {
            FinAdvance a = advances.get(r.advanceId());
            if (a == null || a.getKind() != req.kind() || !a.getPartnerId().equals(req.partnerId())) {
                throw new BizException("fin.error.advanceNotBelongPartner");
            }
            if (!seen.add(a.getId())) {
                throw new BizException("fin.error.duplicateDocument", a.getAdvanceNo());
            }
            String advanceCurrency = a.getCurrency() == null ? FinConstants.BASE_CURRENCY : a.getCurrency();
            if (!advanceCurrency.equals(currency)) {
                throw new BizException("fin.error.settlementAdvanceCurrencyMismatch", a.getAdvanceNo(), advanceCurrency, currency);
            }
            BigDecimal remaining = Decimals.nz(a.getAmountFc()).subtract(Decimals.nz(a.getUsedFc()));
            if (r.amount().compareTo(remaining) > 0) {
                throw new BizException("fin.error.totalOffsetAmountExceedsAdvanceBalance", a.getAdvanceNo(), remaining.toPlainString());
            }
            total = total.add(r.amount());
        }
        return total;
    }

    /**
     * 冲抵的预收预付分配到蓝字单据：按单据日期（同日按到期日）从早到晚，每张不超过其核销金额（不含折让），
     * 红字单据只能用实收实付冲抵
     *
     * @return 单据 ID → 冲抵金额（原币）
     */
    private static Map<Long, BigDecimal> allocateOffsets(List<SettlementItemRequest> items, Map<Long, FinBill> bills, BigDecimal total) {
        Map<Long, BigDecimal> result = new HashMap<>();
        BigDecimal left = total;
        List<SettlementItemRequest> blue = items.stream().filter(i -> i.amount().signum() > 0)
                .sorted(java.util.Comparator.comparing((SettlementItemRequest i) -> bills.get(i.billId()).getBillDate())
                        .thenComparing(i -> bills.get(i.billId()).getDueDate(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(SettlementItemRequest::billId))
                .toList();
        for (SettlementItemRequest i : blue) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal v = left.min(i.amount());
            result.put(i.billId(), v);
            left = left.subtract(v);
        }
        if (left.signum() > 0) {
            throw new BizException("fin.error.advanceOffsetExceedsSettled", total.toPlainString(), total.subtract(left).toPlainString());
        }
        return result;
    }

    /**
     * 确认：付款发布了审批流程时校验最新余额后提交审批（审批通过后核销），否则立即生效
     */
    private void doConfirm(FinSettlement s) {
        periodGuard.checkOpen(s.getSettleDate(), s.getKind() == SettleKind.RECEIPT ? "fin.label.receipt" : "fin.label.payment");
        boolean approval = s.getKind() == SettleKind.PAYMENT && approvalSupport.isAvailable(PAYMENT_FLOW);
        if (!approval) {
            effectuate(s);
            StatusTransitions.change(settlementMapper, FinSettlement::getId, s.getId(), FinSettlement::getStatus,
                    ApproveStatus.DRAFT, ApproveStatus.APPROVED, "fin.error.settlementNotDraft", s.getSettleNo());
            s.setStatus(ApproveStatus.APPROVED);
            return;
        }
        checkOutstanding(s);
        StatusTransitions.change(settlementMapper, FinSettlement::getId, s.getId(), FinSettlement::getStatus,
                ApproveStatus.DRAFT, ApproveStatus.APPROVING, "fin.error.settlementNotDraft", s.getSettleNo());
        s.setStatus(ApproveStatus.APPROVING);
        // 金额为实际付出的本位币（实付 + 多付转预付），不能只按核销合计，否则可以把大额付款做成预付绕过金额条件
        BigDecimal paid = Decimals.money(s.getAmount().add(Decimals.nz(s.getAdvanceAmount()).multiply(s.getExchangeRate())));
        approvalSupport.start(PAYMENT_FLOW, s.getId(), Map.of(
                        "amount", paid.doubleValue(),
                        "settleNo", s.getSettleNo(),
                        "partnerId", s.getPartnerId()),
                () -> {
                },
                instanceId -> settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getFlowInstanceId, instanceId)
                        .eq(FinSettlement::getId, s.getId())));
    }

    /** 提交审批前按最新余额校验：核销 + 冲抵 + 折让不超过单据的未核销金额，冲抵不超过预收预付的余额 */
    private void checkOutstanding(FinSettlement s) {
        List<FinSettlementItem> items = itemsOf(s.getId());
        Map<Long, FinBill> bills = new HashMap<>();
        billMapper.selectByIds(items.stream().map(FinSettlementItem::getBillId).toList()).forEach(b -> bills.put(b.getId(), b));
        for (FinSettlementItem item : items) {
            FinBill bill = bills.get(item.getBillId());
            if (bill == null) {
                throw new BizException("fin.error.settledDocumentNotFound", item.getBillId());
            }
            boolean foreign = ForeignAmounts.isForeign(bill.getCurrency());
            BigDecimal remaining = foreign ? bill.getAmountFc().subtract(bill.getSettledFc()) : bill.getAmount().subtract(bill.getSettledAmount());
            BigDecimal used = Decimals.nz(item.getAmountFc()).add(discountFc(item)).add(Decimals.nz(item.getAdvanceOffset()));
            if (used.abs().compareTo(remaining.abs()) > 0) {
                throw new BizException("fin.error.settledAmountExceedsOutstandingAmount", bill.getBillNo(), remaining.toPlainString());
            }
        }
        for (FinSettlementAdvance sa : settlementAdvanceMapper.selectBySettlement(s.getId())) {
            BigDecimal remaining = Decimals.nz(sa.getAdvanceAmount()).subtract(Decimals.nz(sa.getUsedAmount()));
            if (sa.getAmount().compareTo(remaining) > 0) {
                throw new BizException("fin.error.totalOffsetAmountExceedsAdvanceBalance", sa.getAdvanceNo(), remaining.toPlainString());
            }
        }
    }

    /**
     * 生效（确认或付款审批通过，同一事务）：先按明细的冲抵金额冲抵预收预付（{@link FinAdvanceService#applyForSettlement}，凭证并入收付款凭证），
     * 再核销实收实付和折让（本位币按单据最新余额折算，原币核销完时取剩余本位币），最后把多收 / 多付转为预收预付
     */
    private void effectuate(FinSettlement s) {
        List<FinSettlementItem> items = itemsOf(s.getId());
        List<FinSettlementAdvance> advances = settlementAdvanceMapper.selectList(Wrappers.<FinSettlementAdvance>lambdaQuery()
                .eq(FinSettlementAdvance::getSettlementId, s.getId()).orderByAsc(FinSettlementAdvance::getId));
        applyAdvances(s, items, advances);
        Map<Long, FinBill> bills = new HashMap<>();
        billMapper.selectByIds(items.stream().map(FinSettlementItem::getBillId).toList()).forEach(b -> bills.put(b.getId(), b));
        BigDecimal cashBase = BigDecimal.ZERO;
        BigDecimal discountBase = BigDecimal.ZERO;
        for (FinSettlementItem item : items) {
            FinBill bill = bills.get(item.getBillId());
            if (bill == null) {
                throw new BizException("fin.error.settledDocumentNotFound", item.getBillId());
            }
            BigDecimal fc = item.getAmountFc() == null ? item.getAmount() : item.getAmountFc();
            BigDecimal discFc = discountFc(item);
            if (fc.signum() == 0 && discFc.signum() == 0) {
                // 整张由预收预付冲抵
                continue;
            }
            BigDecimal[] bases = ForeignAmounts.toBase(bill, fc, discFc);
            if (bases[0].compareTo(item.getAmount()) != 0 || bases[1].compareTo(Decimals.nz(item.getDiscount())) != 0) {
                itemMapper.update(Wrappers.<FinSettlementItem>lambdaUpdate().set(FinSettlementItem::getAmount, bases[0])
                        .set(FinSettlementItem::getDiscount, bases[1]).eq(FinSettlementItem::getId, item.getId()));
            }
            settle(bill, bases[0], fc, bases[1], discFc);
            cashBase = cashBase.add(bases[0]);
            discountBase = discountBase.add(bases[1]);
        }
        boolean foreign = ForeignAmounts.isForeign(s.getCurrency());
        if (discountBase.compareTo(Decimals.nz(s.getDiscountAmount())) != 0 || (!foreign && cashBase.compareTo(s.getAmount()) != 0)) {
            s.setDiscountAmount(discountBase);
            if (!foreign) {
                s.setAmount(cashBase);
            }
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getDiscountAmount, discountBase)
                    .set(FinSettlement::getAmount, s.getAmount()).eq(FinSettlement::getId, s.getId()));
        }
        toAdvance(s);
    }

    /** 选用的预收预付按收付日期依次分配到明细的冲抵金额（明细按单据日期），每张预收预付冲抵一次 */
    private void applyAdvances(FinSettlement s, List<FinSettlementItem> items, List<FinSettlementAdvance> advances) {
        if (advances.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> need = new java.util.LinkedHashMap<>();
        items.stream().filter(i -> Decimals.nz(i.getAdvanceOffset()).signum() > 0)
                .forEach(i -> need.put(i.getBillId(), i.getAdvanceOffset()));
        for (FinSettlementAdvance sa : advances) {
            BigDecimal left = sa.getAmount();
            List<SettlementItemRequest> lines = new java.util.ArrayList<>();
            for (Map.Entry<Long, BigDecimal> e : need.entrySet()) {
                if (left.signum() <= 0) {
                    break;
                }
                if (e.getValue().signum() <= 0) {
                    continue;
                }
                BigDecimal v = left.min(e.getValue());
                lines.add(new SettlementItemRequest(e.getKey(), v));
                e.setValue(e.getValue().subtract(v));
                left = left.subtract(v);
            }
            if (!lines.isEmpty()) {
                advanceService.applyForSettlement(sa.getAdvanceId(), s.getSettleDate(), lines, s.getId());
            }
        }
    }

    private List<FinSettlementItem> itemsOf(Long settlementId) {
        return itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                .eq(FinSettlementItem::getSettlementId, settlementId).orderByAsc(FinSettlementItem::getId));
    }

    /** 付款审批结束：通过 → 核销应付单、冲抵预付（超过未核销金额时审批失败）；拒绝、撤销 → 已拒绝，可修改后再次提交或删除 */
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
            effectuate(s);
        }
        StatusTransitions.tryChange(settlementMapper, FinSettlement::getId, s.getId(), FinSettlement::getStatus,
                ApproveStatus.APPROVING, approved ? ApproveStatus.APPROVED : ApproveStatus.REJECTED);
    }

    /**
     * 本次收付款总额超过实收实付（核销合计 − 冲抵预收预付）的部分（多收 / 多付），须选择转为预收 / 预付；总额小于实收实付时报错。外币为原币
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

    private static String currencyOf(FinBill b) {
        return b.getCurrency() == null ? FinConstants.BASE_CURRENCY : b.getCurrency();
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
            // 草稿、已拒绝的没有核销过，不需要恢复
            if (s.getStatus() == ApproveStatus.APPROVED) {
                // 多收转的预收预付一并删除（已冲抵或凭证已审核时不能删除）
                if (s.getAdvanceId() != null) {
                    advanceService.delete(s.getAdvanceId());
                }
                for (FinSettlementItem item : itemsOf(id)) {
                    BigDecimal discount = Decimals.nz(item.getDiscount());
                    BigDecimal fc = (item.getAmountFc() == null ? item.getAmount() : item.getAmountFc()).add(discountFc(item));
                    if (item.getAmount().add(discount).signum() == 0 && fc.signum() == 0) {
                        continue;
                    }
                    billMapper.addSettled(item.getBillId(), item.getAmount().add(discount).negate());
                    billMapper.addSettledFc(item.getBillId(), fc.negate());
                    if (discount.signum() != 0) {
                        billMapper.addDiscount(item.getBillId(), discount.negate());
                    }
                }
                // 收付款中冲抵的预收预付一并撤销
                advanceService.revokeForSettlement(id);
            }
            itemMapper.delete(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getSettlementId, id));
            settlementAdvanceMapper.delete(Wrappers.<FinSettlementAdvance>lambdaQuery().eq(FinSettlementAdvance::getSettlementId, id));
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
            List<FinSettlementItem> items = itemsOf(s.getId());
            BigDecimal booked = Decimals.sum(items, FinSettlementItem::getAmount);
            BigDecimal discount = Decimals.nz(s.getDiscountAmount());
            // 收付款中冲抵的预收预付：预收预付一方按其汇率（amount），单据一方按记账汇率（billAmount）
            List<FinAdvanceApply> applies = applyMapper.selectList(Wrappers.<FinAdvanceApply>lambdaQuery()
                    .eq(FinAdvanceApply::getSettlementId, s.getId()));
            BigDecimal advanceBase = Decimals.sum(applies, FinAdvanceApply::getAmount);
            BigDecimal advanceBillBase = Decimals.sum(applies, FinAdvanceApply::getBillAmount);
            List<VoucherLine> lines = new java.util.ArrayList<>();
            if (s.getAmount().signum() == 0 && discount.signum() == 0 && booked.signum() == 0 && applies.isEmpty()) {
                // 红字单据（冲账、退货）恰好抵消欠款：往来科目按蓝字、红字分两行冲抵，没有资金收付
                BigDecimal blue = Decimals.sum(items.stream().filter(i -> i.getAmount().signum() > 0).toList(), FinSettlementItem::getAmount);
                lines.add(partnerLine(receipt, summary, partnerAccount, s.getPartnerId(), blue));
                lines.add(partnerLine(receipt, summary, partnerAccount, s.getPartnerId(), blue.negate()));
            } else if (receipt) {
                lines.add(VoucherLine.debit(summary, s.getAccountId(), null, s.getAmount()));
                // 收款折让：借 收款折让科目（默认财务费用）
                lines.add(VoucherLine.debit(summary + " 折让", accountRules.accountId(AccountRole.RECEIPT_DISCOUNT), null, discount)); // i18n-ignore
                // 冲抵预收：借 预收账款
                lines.add(VoucherLine.debit(summary + " 冲抵预收", accountRules.accountId(AccountRole.ADVANCE_RECEIVED), null, advanceBase) // i18n-ignore
                        .withCustomer(s.getPartnerId()));
                lines.add(partnerLine(true, summary, partnerAccount, s.getPartnerId(), booked.add(discount).add(advanceBillBase)));
            } else {
                lines.add(partnerLine(false, summary, partnerAccount, s.getPartnerId(), booked.add(discount).add(advanceBillBase)));
                lines.add(VoucherLine.credit(summary, s.getAccountId(), null, s.getAmount()));
                // 付款折让：贷 付款折让科目（默认财务费用）
                lines.add(VoucherLine.credit(summary + " 折让", accountRules.accountId(AccountRole.PAYMENT_DISCOUNT), null, discount)); // i18n-ignore
                // 冲抵预付：贷 预付账款
                lines.add(VoucherLine.credit(summary + " 冲抵预付", accountRules.accountId(AccountRole.PREPAYMENT), null, advanceBase) // i18n-ignore
                        .withSupplier(s.getPartnerId()));
            }
            BigDecimal paid = s.getAmount().add(advanceBase);
            BigDecimal settled = booked.add(advanceBillBase);
            BigDecimal diff = receipt ? paid.subtract(settled) : settled.subtract(paid);
            if (diff.signum() != 0) {
                // 收款多收 / 付款少付为汇兑收益，冲减财务费用
                lines.add(VoucherLine.credit(summary + " 汇兑损益", // i18n-ignore
                        accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, diff));
            }
            Long voucherId = voucherService.createGenerated(s.getSettleDate(), receipt ? VoucherSource.RECEIPT : VoucherSource.PAYMENT,
                    s.getSettleNo(), lines);
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getVoucherId, voucherId).eq(FinSettlement::getId, s.getId()));
            if (!applies.isEmpty()) {
                applyMapper.update(Wrappers.<FinAdvanceApply>lambdaUpdate().set(FinAdvanceApply::getVoucherId, voucherId)
                        .eq(FinAdvanceApply::getSettlementId, s.getId()));
            }
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
