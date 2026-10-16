package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinAdvanceApply;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.mapper.FinAdvanceApplyMapper;
import com.lemon.erp.modules.fin.mapper.FinAdvanceMapper;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
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
 * 发货、收货形成应收应付单后冲抵：借 预收账款 / 贷 应收账款，借 应付账款 / 贷 预付账款，并核销应收应付单
 */
@Service
@RequiredArgsConstructor
public class FinAdvanceService {

    private final FinAdvanceMapper advanceMapper;
    private final FinAdvanceApplyMapper applyMapper;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;

    public PageResult<FinAdvance> page(long current, long size, String kind, Long partnerId, String keyword, boolean onlyOpen) {
        Page<FinAdvance> page = Page.of(current, size);
        advanceMapper.selectPageList(page, kind, partnerId, keyword, onlyOpen);
        return PageResult.of(page);
    }

    public FinAdvance detail(Long id) {
        FinAdvance a = advanceMapper.selectDetail(id);
        if (a == null) {
            throw new BizException("预收预付单不存在");
        }
        a.setApplies(advanceMapper.selectApplies(id));
        return a;
    }

    @Transactional
    public Long create(AdvanceRequest req) {
        boolean receipt = "RECEIPT".equals(req.kind());
        periodGuard.checkOpen(req.advanceDate(), receipt ? "预收款" : "预付款");
        accountService.requireCashAccount(req.accountId());
        String partner = billService.partnerName(receipt ? "AR" : "AP", req.partnerId());
        FinAdvance a = new FinAdvance();
        a.setAdvanceNo(docNoService.next(receipt ? "YSK" : "YFK"));
        a.setKind(req.kind());
        a.setPartnerId(req.partnerId());
        a.setAdvanceDate(req.advanceDate());
        a.setAccountId(req.accountId());
        a.setMethod(req.method());
        a.setAmount(req.amount());
        a.setUsedAmount(BigDecimal.ZERO);
        a.setRemark(req.remark());
        a.setCreateBy(SecurityUtils.getUserId());
        advanceMapper.insert(a);
        String summary = (receipt ? "预收 " : "预付 ") + partner + " 货款";
        List<VoucherLine> lines = receipt
                ? List.of(VoucherLine.debit(summary, req.accountId(), null, req.amount()),
                VoucherLine.credit(summary, accountService.leafByCode(FinConstants.ADVANCE_RECEIVED).getId(), null, req.amount())
                        .withCustomer(req.partnerId()))
                : List.of(VoucherLine.debit(summary, accountService.leafByCode(FinConstants.PREPAYMENT).getId(), null, req.amount())
                        .withSupplier(req.partnerId()),
                VoucherLine.credit(summary, req.accountId(), null, req.amount()));
        Long voucherId = voucherService.createGenerated(req.advanceDate(), VoucherSource.ADVANCE, a.getAdvanceNo(), lines);
        advanceMapper.update(Wrappers.<FinAdvance>lambdaUpdate().set(FinAdvance::getVoucherId, voucherId).eq(FinAdvance::getId, a.getId()));
        return a.getId();
    }

    /** 冲抵应收应付单：每张单据冲抵金额不超过其未核销金额，合计不超过预收预付余额 */
    @Transactional
    public void apply(Long id, LocalDate date, List<SettlementItemRequest> items) {
        FinAdvance a = detail(id);
        boolean receipt = "RECEIPT".equals(a.getKind());
        String billKind = receipt ? "AR" : "AP";
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "预收预付冲抵");
        Map<Long, FinBill> bills = billMapper.selectByIds(items.stream().map(SettlementItemRequest::billId).toList()).stream()
                .collect(Collectors.toMap(FinBill::getId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        List<String> billNos = new ArrayList<>();
        for (SettlementItemRequest item : items) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || !bill.getKind().equals(billKind) || !bill.getPartnerId().equals(a.getPartnerId())) {
                throw new BizException("冲抵的单据不属于该" + (receipt ? "客户" : "供应商"));
            }
            if (bill.getCurrency() != null && !"CNY".equals(bill.getCurrency())) {
                throw new BizException(bill.getBillNo() + " 为外币单据，请用收付款核销");
            }
            if (!seen.add(bill.getId()) || item.amount().signum() <= 0 || bill.getAmount().signum() <= 0) {
                throw new BizException(bill.getBillNo() + "：冲抵金额必须大于 0，且单据不能重复、不能是红字单据");
            }
            if (billMapper.addSettled(bill.getId(), item.amount()) == 0) {
                throw new BizException(bill.getBillNo() + "：冲抵金额超过未核销金额 " + bill.getAmount().subtract(bill.getSettledAmount()).toPlainString());
            }
            billMapper.addSettledFc(bill.getId(), item.amount());
            total = total.add(item.amount());
            billNos.add(bill.getBillNo());
        }
        if (total.signum() <= 0 || advanceMapper.addUsed(id, total) == 0) {
            throw new BizException("冲抵金额合计超过预收预付余额 " + a.getAmount().subtract(a.getUsedAmount()).toPlainString());
        }
        String summary = (receipt ? "预收款冲抵应收 " : "预付款冲抵应付 ") + a.getAdvanceNo() + " " + String.join("、", billNos);
        if (summary.length() > 200) {
            summary = summary.substring(0, 200);
        }
        List<VoucherLine> lines = receipt
                ? List.of(VoucherLine.debit(summary, accountService.leafByCode(FinConstants.ADVANCE_RECEIVED).getId(), null, total)
                        .withCustomer(a.getPartnerId()),
                VoucherLine.credit(summary, accountService.leafByCode(FinConstants.RECEIVABLE).getId(), null, total)
                        .withCustomer(a.getPartnerId()))
                : List.of(VoucherLine.debit(summary, accountService.leafByCode(FinConstants.PAYABLE).getId(), null, total)
                        .withSupplier(a.getPartnerId()),
                VoucherLine.credit(summary, accountService.leafByCode(FinConstants.PREPAYMENT).getId(), null, total)
                        .withSupplier(a.getPartnerId()));
        Long voucherId = voucherService.createGenerated(d, VoucherSource.ADVANCE, a.getAdvanceNo(), lines);
        for (SettlementItemRequest item : items) {
            FinAdvanceApply apply = new FinAdvanceApply();
            apply.setAdvanceId(id);
            apply.setBillId(item.billId());
            apply.setAmount(item.amount());
            apply.setApplyDate(d);
            apply.setVoucherId(voucherId);
            apply.setCreateBy(SecurityUtils.getUserId());
            applyMapper.insert(apply);
        }
    }

    /** 删除：未冲抵且凭证未审核的预收预付单，同时删除其凭证 */
    @Transactional
    public void delete(Long id) {
        FinAdvance a = detail(id);
        if (a.getUsedAmount().signum() != 0) {
            throw new BizException("已冲抵的预收预付单不能删除");
        }
        if (a.getVoucherId() != null) {
            voucherService.deleteOwned(a.getVoucherId());
        }
        advanceMapper.deleteById(id);
    }

    /** 往来单位未冲抵的预收（客户）余额，信用额度占用时扣减 */
    public BigDecimal unusedReceipt(Long customerId) {
        BigDecimal v = advanceMapper.sumUnused("RECEIPT", customerId);
        return v == null ? BigDecimal.ZERO : v;
    }
}
