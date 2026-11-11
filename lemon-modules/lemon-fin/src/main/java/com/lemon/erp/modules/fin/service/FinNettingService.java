package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.dto.NettingRequests;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinNetting;
import com.lemon.erp.modules.fin.entity.FinNettingItem;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinNettingItemMapper;
import com.lemon.erp.modules.fin.mapper.FinNettingMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应收应付对冲（issue #95）：同一单位既是客户又是供应商（供应商资料中设置了关联客户）时，用应付抵减应收，不收付现金。
 * <ul>
 *     <li>选择客户（取其关联的供应商），勾选要核销的应收单和应付单并填写核销金额，两边合计必须相等（即对冲金额）</li>
 *     <li>只能核销本位币的单据（外币单据先按收付款处理），金额与单据同号、不超过未核销余额，红字单据可以一起选</li>
 *     <li>保存即生效：单据按核销金额增加已核销，生成凭证（未审核）借 应付账款（供应商）/ 贷 应收账款（客户）</li>
 *     <li>删除：凭证未审核、期间未结账时可以删除，恢复单据的已核销并删除凭证；凭证不能单独删除</li>
 * </ul>
 * 往来汇总、账龄、往来对账单、客户信用占用都按核销后的余额计算
 */
@Service
@RequiredArgsConstructor
public class FinNettingService {

    /** 对冲单号的编码规则标识 */
    public static final String DOC_NO = "DC";

    private final FinNettingMapper nettingMapper;
    private final FinNettingItemMapper itemMapper;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinAccountRuleService accountRules;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;
    private final ScmSupplierService supplierService;
    private final ScmCustomerService customerService;

    public PageResult<FinNetting> page(long current, long size, NettingRequests.Query query) {
        Page<FinNetting> page = Page.of(current, size);
        nettingMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public FinNetting detail(Long id) {
        FinNetting n = nettingMapper.selectDetail(id);
        if (n == null) {
            throw new BizException("fin.error.nettingNotFound");
        }
        n.setItems(nettingMapper.selectItems(id));
        return n;
    }

    /** 新建对冲单的候选：客户关联的供应商、两边未核销的本位币单据 */
    public NettingRequests.Candidates candidates(Long customerId) {
        customerService.get(customerId);
        ScmSupplier supplier = requireLinked(customerId);
        return new NettingRequests.Candidates(supplier.getId(), supplier.getCode(), supplier.getName(),
                baseCurrencyOnly(billService.unsettled(BillKind.AR, customerId)),
                baseCurrencyOnly(billService.unsettled(BillKind.AP, supplier.getId())));
    }

    /** @return 新对冲单 ID */
    @Transactional
    public Long create(NettingRequests.Save req) {
        customerService.get(req.customerId());
        ScmSupplier supplier = requireLinked(req.customerId());
        periodGuard.checkOpen(req.nettingDate(), "fin.label.nettingDoc");
        if (req.items().stream().map(NettingRequests.Item::billId).collect(Collectors.toCollection(HashSet::new)).size() != req.items().size()) {
            throw new BizException("fin.error.nettingDuplicateBill");
        }
        Map<Long, FinBill> bills = billMapper.selectByIds(req.items().stream().map(NettingRequests.Item::billId).toList()).stream()
                .collect(Collectors.toMap(FinBill::getId, Function.identity()));
        BigDecimal ar = BigDecimal.ZERO;
        BigDecimal ap = BigDecimal.ZERO;
        List<FinNettingItem> items = new ArrayList<>();
        for (NettingRequests.Item i : req.items()) {
            FinBill bill = bills.get(i.billId());
            BigDecimal amount = Decimals.money(i.amount());
            check(bill, amount, req.customerId(), supplier.getId());
            if (bill.getKind() == BillKind.AR) {
                ar = ar.add(amount);
            } else {
                ap = ap.add(amount);
            }
            FinNettingItem item = new FinNettingItem();
            item.setBillId(bill.getId());
            item.setKind(bill.getKind());
            item.setAmount(amount);
            items.add(item);
        }
        if (ar.signum() <= 0 || ar.compareTo(ap) != 0) {
            throw new BizException("fin.error.nettingNotBalanced", ar.toPlainString(), ap.toPlainString());
        }
        FinNetting n = new FinNetting();
        n.setNettingNo(docNoService.next(DOC_NO));
        n.setCustomerId(req.customerId());
        n.setSupplierId(supplier.getId());
        n.setNettingDate(req.nettingDate());
        n.setAmount(ar);
        n.setRemark(req.remark());
        nettingMapper.insert(n);
        items.forEach(i -> i.setNettingId(n.getId()));
        nettingMapper.insertItems(items);
        for (FinNettingItem i : items) {
            // 按条件累加，并发核销超过余额时更新不到
            if (billMapper.addSettled(i.getBillId(), i.getAmount()) == 0) {
                throw new BizException("fin.error.nettingExceedsRemaining", bills.get(i.getBillId()).getBillNo());
            }
            billMapper.addSettledFc(i.getBillId(), i.getAmount());
        }
        String customerName = billService.partnerName(BillKind.AR, req.customerId());
        String summary = Messages.get(Messages.DEFAULT_LOCALE, "fin.text.nettingSummary", customerName, supplier.getName());
        Long voucherId = voucherService.createGenerated(req.nettingDate(), VoucherSource.NETTING, n.getNettingNo(), List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.PAYABLE), null, ar).withSupplier(supplier.getId()),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.RECEIVABLE), null, ar).withCustomer(req.customerId())));
        nettingMapper.update(Wrappers.<FinNetting>lambdaUpdate().set(FinNetting::getVoucherId, voucherId).eq(FinNetting::getId, n.getId()));
        return n.getId();
    }

    /** 删除：凭证未审核、期间未结账时，恢复单据的已核销，删除凭证和对冲单 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinNetting n = nettingMapper.selectById(id);
            if (n == null) {
                continue;
            }
            periodGuard.checkOpen(n.getNettingDate(), "fin.label.nettingDoc");
            if (n.getVoucherId() != null) {
                // 凭证已审核、已记账时抛出提示（先反审核或红字冲销）
                voucherService.deleteOwned(n.getVoucherId());
            }
            for (FinNettingItem i : itemMapper.selectList(Wrappers.<FinNettingItem>lambdaQuery().eq(FinNettingItem::getNettingId, id))) {
                billMapper.addSettled(i.getBillId(), i.getAmount().negate());
                billMapper.addSettledFc(i.getBillId(), i.getAmount().negate());
            }
            itemMapper.delete(Wrappers.<FinNettingItem>lambdaQuery().eq(FinNettingItem::getNettingId, id));
            nettingMapper.deleteById(id);
        }
    }

    private ScmSupplier requireLinked(Long customerId) {
        ScmSupplier supplier = supplierService.linkedSupplier(customerId);
        if (supplier == null) {
            throw new BizException("fin.error.nettingNoLinkedSupplier");
        }
        return supplier;
    }

    /** 单据属于该客户（应收）或其关联供应商（应付）、本位币、未核销完，金额与余额同号且不超过余额 */
    private static void check(FinBill bill, BigDecimal amount, Long customerId, Long supplierId) {
        if (bill == null || !(bill.getKind() == BillKind.AR && Objects.equals(bill.getPartnerId(), customerId)
                || bill.getKind() == BillKind.AP && Objects.equals(bill.getPartnerId(), supplierId))) {
            throw new BizException("fin.error.nettingBillNotOfPartner");
        }
        if (bill.getCurrency() != null && !FinConstants.BASE_CURRENCY.equals(bill.getCurrency())) {
            throw new BizException("fin.error.nettingForeignBill", bill.getBillNo());
        }
        BigDecimal remaining = bill.getAmount().subtract(bill.getSettledAmount());
        if (bill.getStatus() == BillStatus.SETTLED || amount.signum() == 0 || amount.signum() != remaining.signum()
                || amount.abs().compareTo(remaining.abs()) > 0) {
            throw new BizException("fin.error.nettingExceedsRemaining", bill.getBillNo());
        }
    }

    private static List<FinBill> baseCurrencyOnly(List<FinBill> bills) {
        return bills.stream().filter(b -> b.getCurrency() == null || FinConstants.BASE_CURRENCY.equals(b.getCurrency())).toList();
    }
}
