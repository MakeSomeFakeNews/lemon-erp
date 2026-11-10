package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinAdvanceApply;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.mapper.FinAdvanceApplyMapper;
import com.lemon.erp.modules.fin.mapper.FinAdvanceMapper;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementAdvanceMapper;
import com.lemon.erp.modules.fin.entity.FinSettlementAdvance;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预收预付：收到客户预付款记 借 银行 / 贷 预收账款，预付供应商记 借 预付账款 / 贷 银行（立即生成凭证）；
 * 发货、收货形成应收应付单后冲抵：借 预收账款 / 贷 应收账款，借 应付账款 / 贷 预付账款，并核销应收应付单。
 * 外币预收预付按原币登记（本位币 = 原币 × 收付日汇率），只能冲抵同币种的单据：冲抵金额为原币，预收预付按其汇率、
 * 单据按记账汇率折算本位币，差额计入财务费用（汇兑损益）
 */
@Service
@RequiredArgsConstructor
public class FinAdvanceService {

    private final FinAdvanceMapper advanceMapper;
    private final FinAdvanceApplyMapper applyMapper;
    private final FinSettlementAdvanceMapper settlementAdvanceMapper;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;
    private final ExchangeRateProvider rateProvider;

    public PageResult<FinAdvance> page(long current, long size, SettleKind kind, Long partnerId, String keyword, boolean onlyOpen) {
        Page<FinAdvance> page = Page.of(current, size);
        advanceMapper.selectPageList(page, kind, partnerId, keyword, onlyOpen);
        return PageResult.of(page);
    }

    public FinAdvance detail(Long id) {
        FinAdvance a = advanceMapper.selectDetail(id);
        if (a == null) {
            throw new BizException("fin.error.advanceDocumentDoesNotExist");
        }
        a.setApplies(advanceMapper.selectApplies(id));
        return a;
    }

    @Transactional
    public Long create(AdvanceRequest req) {
        boolean receipt = req.kind() == SettleKind.RECEIPT;
        periodGuard.checkOpen(req.advanceDate(), receipt ? "fin.label.advanceReceipt" : "fin.label.advancePayment");
        accountService.requireCashAccount(req.accountId());
        String partner = billService.partnerName(req.kind(), req.partnerId());
        String currency = req.currency() == null || req.currency().isBlank() ? FinConstants.BASE_CURRENCY : req.currency().trim();
        boolean foreign = ForeignAmounts.isForeign(currency);
        BigDecimal rate = !foreign ? BigDecimal.ONE : req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.advanceDate());
        if (rate == null) {
            throw new BizException("fin.error.enterExchangeRateAdvance", currency);
        }
        // 本位币金额：外币按收付日汇率折算，凭证按本位币
        BigDecimal base = foreign ? Decimals.money(req.amount().multiply(rate)) : req.amount();
        FinAdvance a = new FinAdvance();
        a.setAdvanceNo(docNoService.next(receipt ? FinDocNos.ADVANCE_RECEIPT : FinDocNos.ADVANCE_PAYMENT));
        a.setKind(req.kind());
        a.setPartnerId(req.partnerId());
        a.setAdvanceDate(req.advanceDate());
        a.setAccountId(req.accountId());
        a.setMethod(req.method());
        a.setAmount(base);
        a.setUsedAmount(BigDecimal.ZERO);
        a.setCurrency(currency);
        a.setExchangeRate(rate);
        a.setAmountFc(req.amount());
        a.setUsedFc(BigDecimal.ZERO);
        a.setRemark(req.remark());
        advanceMapper.insert(a);
        String summary = (receipt ? "预收 " : "预付 ") + partner + " 货款" // i18n-ignore 凭证摘要
                + (foreign ? " " + currency + " " + req.amount().toPlainString() + " @" + rate.stripTrailingZeros().toPlainString() : "");
        List<VoucherLine> lines = receipt
                ? List.of(VoucherLine.debit(summary, req.accountId(), null, base),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.ADVANCE_RECEIVED), null, base)
                        .withCustomer(req.partnerId()))
                : List.of(VoucherLine.debit(summary, accountRules.accountId(AccountRole.PREPAYMENT), null, base)
                        .withSupplier(req.partnerId()),
                VoucherLine.credit(summary, req.accountId(), null, base));
        Long voucherId = voucherService.createGenerated(req.advanceDate(), VoucherSource.ADVANCE, a.getAdvanceNo(), lines);
        advanceMapper.update(Wrappers.<FinAdvance>lambdaUpdate().set(FinAdvance::getVoucherId, voucherId).eq(FinAdvance::getId, a.getId()));
        return a.getId();
    }

    /**
     * 冲抵应收应付单：每张单据冲抵金额不超过其未核销金额，合计不超过预收预付余额。
     * 外币预收预付只能冲抵同币种的单据，冲抵金额为原币
     */
    @Transactional
    public void apply(Long id, LocalDate date, List<SettlementItemRequest> items) {
        FinAdvance a = detail(id);
        boolean receipt = a.getKind() == SettleKind.RECEIPT;
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "fin.label.advanceOffset");
        Applied r = doApply(a, items);
        String summary = (receipt ? "预收款冲抵应收 " : "预付款冲抵应付 ") + a.getAdvanceNo() + " " + String.join("、", r.billNos()); // i18n-ignore 凭证摘要
        if (summary.length() > 200) {
            summary = summary.substring(0, 200);
        }
        List<VoucherLine> lines = new ArrayList<>(receipt
                ? List.of(VoucherLine.debit(summary, accountRules.accountId(AccountRole.ADVANCE_RECEIVED), null, r.advanceBase())
                        .withCustomer(a.getPartnerId()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.RECEIVABLE), null, r.billBase())
                        .withCustomer(a.getPartnerId()))
                : List.of(VoucherLine.debit(summary, accountRules.accountId(AccountRole.PAYABLE), null, r.billBase())
                        .withSupplier(a.getPartnerId()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.PREPAYMENT), null, r.advanceBase())
                        .withSupplier(a.getPartnerId())));
        // 外币：预收预付汇率与单据记账汇率的差额计入财务费用（预收汇率高于记账汇率为汇兑收益，冲减财务费用）
        BigDecimal diff = receipt ? r.advanceBase().subtract(r.billBase()) : r.billBase().subtract(r.advanceBase());
        if (diff.signum() != 0) {
            lines.add(VoucherLine.credit(summary + " 汇兑损益", accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, diff)); // i18n-ignore
        }
        Long voucherId = voucherService.createGenerated(d, VoucherSource.ADVANCE, a.getAdvanceNo(), lines);
        insertApplies(id, items, r, d, voucherId, null);
    }

    /**
     * 在收付款中冲抵（收付款确认 / 付款审批通过时，与核销同一事务）：校验、核销与 {@link #apply} 相同，但不单独生成凭证——
     * 借 预收账款 / 贷 应收账款（付款反之）并入收付款单的凭证；冲抵记录关联收付款单，删除收付款单时撤销
     */
    void applyForSettlement(Long id, LocalDate date, List<SettlementItemRequest> items, Long settlementId) {
        FinAdvance a = advanceMapper.selectById(id);
        if (a == null) {
            throw new BizException("fin.error.advanceDocumentDoesNotExist");
        }
        Applied r = doApply(a, items);
        insertApplies(id, items, r, date, null, settlementId);
    }

    /** 撤销收付款单中的预收预付冲抵（删除已生效的收付款单时）：恢复单据的已核销和预收预付的已冲抵，删除冲抵记录 */
    void revokeForSettlement(Long settlementId) {
        List<FinAdvanceApply> applies = applyMapper.selectList(Wrappers.<FinAdvanceApply>lambdaQuery()
                .eq(FinAdvanceApply::getSettlementId, settlementId));
        for (FinAdvanceApply p : applies) {
            BigDecimal fc = p.getAmountFc() == null ? p.getAmount() : p.getAmountFc();
            if (billMapper.addSettled(p.getBillId(), p.getBillAmount().negate()) == 0
                    || advanceMapper.addUsed(p.getAdvanceId(), p.getAmount().negate(), fc.negate()) == 0) {
                throw new BizException("fin.error.dataChangedRefresh");
            }
            billMapper.addSettledFc(p.getBillId(), fc.negate());
            applyMapper.deleteById(p.getId());
        }
    }

    /** 冲抵的校验与核销结果：各单据的单号、单据一方本位币，各冲抵行的预收预付本位币及合计 */
    private record Applied(List<String> billNos, List<BigDecimal> billBases, List<BigDecimal> applyBases,
                           BigDecimal advanceBase, BigDecimal billBase) {
    }

    /** 校验并核销单据、累加预收预付的已冲抵 */
    private Applied doApply(FinAdvance a, List<SettlementItemRequest> items) {
        boolean receipt = a.getKind() == SettleKind.RECEIPT;
        BillKind billKind = a.getKind().billKind();
        String currency = currencyOf(a);
        Map<Long, FinBill> bills = billMapper.selectByIds(items.stream().map(SettlementItemRequest::billId).toList()).stream()
                .collect(Collectors.toMap(FinBill::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        BigDecimal totalFc = BigDecimal.ZERO;
        BigDecimal billBase = BigDecimal.ZERO;
        List<String> billNos = new ArrayList<>();
        List<BigDecimal> billBases = new ArrayList<>();
        for (SettlementItemRequest item : items) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || bill.getKind() != billKind || !bill.getPartnerId().equals(a.getPartnerId())) {
                throw receipt ? new BizException("fin.error.offsetDocumentDoesNotBelongCustomer") : new BizException("fin.error.offsetDocumentDoesNotBelongSupplier");
            }
            String billCurrency = bill.getCurrency() == null ? FinConstants.BASE_CURRENCY : bill.getCurrency();
            if (!currency.equals(billCurrency)) {
                throw new BizException("fin.error.advanceCurrencyMismatch", bill.getBillNo(), billCurrency, currency);
            }
            if (!seen.add(bill.getId()) || item.amount().signum() <= 0 || bill.getAmount().signum() <= 0) {
                throw new BizException("fin.error.offsetAmountMustGreaterDocumentsCannot", bill.getBillNo());
            }
            BigDecimal base = ForeignAmounts.toBase(bill, item.amount(), BigDecimal.ZERO)[0];
            if (billMapper.addSettled(bill.getId(), base) == 0) {
                throw new BizException("fin.error.offsetAmountExceedsOutstandingAmount", bill.getBillNo(),
                        ForeignAmounts.isForeign(currency) ? bill.getAmountFc().subtract(bill.getSettledFc()).toPlainString()
                                : bill.getAmount().subtract(bill.getSettledAmount()).toPlainString());
            }
            billMapper.addSettledFc(bill.getId(), item.amount());
            totalFc = totalFc.add(item.amount());
            billBase = billBase.add(base);
            billBases.add(base);
            billNos.add(bill.getBillNo());
        }
        // 预收预付的本位币：按其汇率折算，原币冲抵完时取剩余本位币
        List<BigDecimal> applyBases = applyBases(a, items, totalFc);
        BigDecimal advanceBase = Decimals.sum(applyBases);
        if (totalFc.signum() <= 0 || advanceMapper.addUsed(a.getId(), advanceBase, totalFc) == 0) {
            throw new BizException("fin.error.totalOffsetAmountExceedsAdvanceBalance", a.getAdvanceNo(),
                    Decimals.nz(a.getAmountFc()).subtract(Decimals.nz(a.getUsedFc())).toPlainString());
        }
        return new Applied(billNos, billBases, applyBases, advanceBase, billBase);
    }

    private void insertApplies(Long advanceId, List<SettlementItemRequest> items, Applied r, LocalDate date, Long voucherId, Long settlementId) {
        for (int i = 0; i < items.size(); i++) {
            FinAdvanceApply apply = new FinAdvanceApply();
            apply.setAdvanceId(advanceId);
            apply.setBillId(items.get(i).billId());
            apply.setSettlementId(settlementId);
            apply.setAmount(r.applyBases().get(i));
            apply.setAmountFc(items.get(i).amount());
            apply.setBillAmount(r.billBases().get(i));
            apply.setApplyDate(date);
            apply.setVoucherId(voucherId);
            applyMapper.insert(apply);
        }
    }

    /** 各冲抵行的预收预付本位币：原币 × 预收预付汇率；本次冲抵完全部原币余额时，最后一行取剩余本位币 */
    private static List<BigDecimal> applyBases(FinAdvance a, List<SettlementItemRequest> items, BigDecimal totalFc) {
        boolean foreign = ForeignAmounts.isForeign(currencyOf(a));
        List<BigDecimal> bases = new ArrayList<>();
        for (SettlementItemRequest item : items) {
            bases.add(foreign ? Decimals.money(item.amount().multiply(a.getExchangeRate())) : item.amount());
        }
        BigDecimal remainingFc = Decimals.nz(a.getAmountFc()).subtract(Decimals.nz(a.getUsedFc()));
        if (foreign && !bases.isEmpty() && totalFc.compareTo(remainingFc) == 0) {
            BigDecimal others = Decimals.sum(bases.subList(0, bases.size() - 1));
            bases.set(bases.size() - 1, a.getAmount().subtract(a.getUsedAmount()).subtract(others));
        }
        return bases;
    }

    private static String currencyOf(FinAdvance a) {
        return a.getCurrency() == null ? FinConstants.BASE_CURRENCY : a.getCurrency();
    }

    /** 删除：未冲抵且凭证未审核的预收预付单，同时删除其凭证 */
    @Transactional
    public void delete(Long id) {
        FinAdvance a = detail(id);
        if (a.getUsedAmount().signum() != 0) {
            throw new BizException("fin.error.advanceHasOffsetCannotDeleted");
        }
        // 被收付款草稿、审批中的付款单选用的不能删除（确认时要冲抵）
        if (settlementAdvanceMapper.exists(Wrappers.<FinSettlementAdvance>lambdaQuery().eq(FinSettlementAdvance::getAdvanceId, id))) {
            throw new BizException("fin.error.advanceSelectedBySettlement", a.getAdvanceNo());
        }
        if (a.getVoucherId() != null) {
            voucherService.deleteOwned(a.getVoucherId());
        }
        advanceMapper.deleteById(id);
    }

    /** 往来单位未冲抵完的预收 / 预付款（收付款中勾选冲抵），按收付日期从早到晚 */
    public List<FinAdvance> open(SettleKind kind, Long partnerId) {
        return advanceMapper.selectOpen(kind, partnerId);
    }

    /** 往来单位未冲抵的预收（客户）余额，信用额度占用时扣减 */
    public BigDecimal unusedReceipt(Long customerId) {
        BigDecimal v = advanceMapper.sumUnused(SettleKind.RECEIPT, customerId);
        return Decimals.nz(v);
    }
}
