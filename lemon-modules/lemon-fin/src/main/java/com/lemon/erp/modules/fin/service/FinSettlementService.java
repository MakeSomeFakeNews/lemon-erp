package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.model.SettleMethod;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.FINANCE_EXPENSE;
import static com.lemon.erp.modules.fin.model.FinConstants.PAYABLE;
import static com.lemon.erp.modules.fin.model.FinConstants.RECEIVABLE;

/**
 * 收款 / 付款：核销同一往来单位的应收 / 应付单（可同时核销红字退货单冲抵），金额为核销合计且必须大于 0。
 * 生成凭证：收款 借 银行存款 / 贷 应收账款；付款 借 应付账款 / 贷 银行存款
 */
@Service
@RequiredArgsConstructor
public class FinSettlementService {

    /** 付款审批流程标识（只有付款单走审批） */
    public static final String PAYMENT_PROCESS_KEY = "fin_payment";
    private static final String BUSINESS_KEY_PREFIX = "fin_payment:";
    private static final String STATUS_APPROVING = "APPROVING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final FinSettlementMapper settlementMapper;
    private final FinSettlementItemMapper itemMapper;
    private final FinBillMapper billMapper;
    private final FinBillService billService;
    private final FinAccountService accountService;
    private final FinVoucherService voucherService;
    private final DocNoService docNoService;
    private final FinBizPeriodGuard periodGuard;
    private final com.lemon.erp.framework.finance.ExchangeRateProvider rateProvider;
    private final FlowApi flowApi;

    public PageResult<FinSettlement> page(long current, long size, String kind, Long partnerId, String keyword) {
        checkKind(kind);
        Page<FinSettlement> page = Page.of(current, size);
        settlementMapper.selectPageList(page, kind, partnerId, keyword);
        return PageResult.of(page);
    }

    public FinSettlement detail(Long id) {
        FinSettlement s = settlementMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("收付款单不存在");
        }
        s.setItems(itemMapper.selectBySettlement(id));
        return s;
    }

    /** @return 新单据 ID */
    @Transactional
    public Long create(SettlementRequest req) {
        accountService.requireCashAccount(req.accountId());
        return doCreate(req);
    }

    /**
     * 以票据结算（收票、背书转让、开具应付票据）：结算科目为应收票据或应付票据，其余与收付款相同（核销往来、付款审批、生成凭证）
     *
     * @param noteAccountCode 应收票据 / 应付票据科目编码
     * @return 新单据 ID
     */
    @Transactional
    public Long createForNote(SettlementRequest req, String noteAccountCode) {
        FinAccount account = accountService.leafByCode(noteAccountCode);
        if (!account.getId().equals(req.accountId())) {
            throw new BizException("票据结算科目不正确");
        }
        return doCreate(req);
    }

    /** 审批中（付款审批未结束） */
    public static boolean isApproving(FinSettlement s) {
        return STATUS_APPROVING.equals(s.getStatus());
    }

    private Long doCreate(SettlementRequest req) {
        checkKind(req.kind());
        periodGuard.checkOpen(req.settleDate(), "RECEIPT".equals(req.kind()) ? "收款单" : "付款单");
        String billKind = "RECEIPT".equals(req.kind()) ? "AR" : "AP";
        // 付款发布了审批流程时先提交审批，审批通过后核销
        boolean approval = "PAYMENT".equals(req.kind()) && flowApi.isAvailable(PAYMENT_PROCESS_KEY);
        Set<Long> seen = new HashSet<>();
        Map<Long, FinBill> bills = billMapper.selectByIds(req.items().stream().map(SettlementItemRequest::billId).toList())
                .stream().collect(Collectors.toMap(FinBill::getId, Function.identity()));
        // 外币：同一张收付款单只能核销同一币种的单据，核销金额为原币
        Set<String> currencies = bills.values().stream().map(b -> b.getCurrency() == null ? "CNY" : b.getCurrency()).collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new BizException("同一张收付款单只能核销同一币种的单据");
        }
        String currency = currencies.isEmpty() ? "CNY" : currencies.iterator().next();
        boolean foreign = !"CNY".equals(currency);
        BigDecimal settleRate = !foreign ? BigDecimal.ONE : req.exchangeRate() != null ? req.exchangeRate() : rateProvider.rate(currency, req.settleDate());
        if (settleRate == null) {
            throw new BizException("请填写 " + currency + " 的收付款汇率");
        }
        Map<Long, BigDecimal> baseAmounts = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalFc = BigDecimal.ZERO;
        for (SettlementItemRequest item : req.items()) {
            FinBill bill = bills.get(item.billId());
            if (bill == null || !bill.getKind().equals(billKind) || !bill.getPartnerId().equals(req.partnerId())) {
                throw new BizException("核销的单据不属于该" + ("AR".equals(billKind) ? "客户" : "供应商"));
            }
            if (!seen.add(item.billId())) {
                throw new BizException("单据重复：" + bill.getBillNo());
            }
            if (item.amount().signum() == 0 || item.amount().signum() != bill.getAmount().signum()) {
                throw new BizException(bill.getBillNo() + "：核销金额必须与单据金额同号且不为 0");
            }
            BigDecimal base = baseOf(bill, item.amount());
            baseAmounts.put(bill.getId(), base);
            if (approval) {
                // 审批通过后才核销，这里只校验当前的未核销金额
                BigDecimal remaining = foreign ? bill.getAmountFc().subtract(bill.getSettledFc()) : bill.getAmount().subtract(bill.getSettledAmount());
                if (item.amount().abs().compareTo(remaining.abs()) > 0) {
                    throw new BizException(bill.getBillNo() + "：核销金额超过未核销金额 " + remaining.toPlainString());
                }
            } else {
                settle(bill, base, item.amount());
            }
            total = total.add(foreign ? base : item.amount());
            totalFc = totalFc.add(item.amount());
        }
        if (totalFc.signum() <= 0) {
            throw new BizException("收付款金额必须大于 0（红字单据冲抵后）");
        }
        FinSettlement s = new FinSettlement();
        s.setSettleNo(docNoService.next("RECEIPT".equals(req.kind()) ? "SK" : "FK"));
        s.setKind(req.kind());
        s.setPartnerId(req.partnerId());
        s.setSettleDate(req.settleDate());
        s.setAccountId(req.accountId());
        s.setMethod(req.method());
        // 外币：实收实付本位币 = 原币 × 收付款汇率，与核销的记账本位币的差额在凭证中计入汇兑损益
        s.setAmount(foreign ? totalFc.multiply(settleRate).setScale(2, java.math.RoundingMode.HALF_UP) : total);
        s.setCurrency(currency);
        s.setExchangeRate(settleRate);
        s.setAmountFc(totalFc);
        s.setRemark(req.remark());
        s.setStatus(approval ? STATUS_APPROVING : STATUS_APPROVED);
        s.setCreateBy(SecurityUtils.getUserId());
        settlementMapper.insert(s);
        itemMapper.insertBatch(req.items().stream().map(i -> {
            FinSettlementItem item = new FinSettlementItem();
            item.setSettlementId(s.getId());
            item.setBillId(i.billId());
            item.setAmount(baseAmounts.get(i.billId()));
            item.setAmountFc(i.amount());
            return item;
        }).toList());
        if (approval) {
            String instanceId = flowApi.start(PAYMENT_PROCESS_KEY, BUSINESS_KEY_PREFIX + s.getId(), Map.of(
                    "amount", total.doubleValue(),
                    "settleNo", s.getSettleNo(),
                    "partnerId", s.getPartnerId()));
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getFlowInstanceId, instanceId)
                    .eq(FinSettlement::getId, s.getId()));
        }
        return s.getId();
    }

    /** 付款审批结束：通过 → 核销应付单（超过未核销金额时审批失败）；拒绝、撤销 → 已拒绝，可删除后重新录入 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        FinSettlement s = settlementMapper.selectById(Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length())));
        if (s == null || !STATUS_APPROVING.equals(s.getStatus())) {
            return;
        }
        boolean approved = e.status() == FlowStatus.APPROVED;
        if (approved) {
            periodGuard.checkOpen(s.getSettleDate(), "付款单");
            Map<Long, FinBill> bills = new HashMap<>();
            List<FinSettlementItem> items = itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                    .eq(FinSettlementItem::getSettlementId, s.getId()));
            billMapper.selectByIds(items.stream().map(FinSettlementItem::getBillId).toList()).forEach(b -> bills.put(b.getId(), b));
            for (FinSettlementItem item : items) {
                FinBill bill = bills.get(item.getBillId());
                if (bill == null) {
                    throw new BizException("核销的单据不存在：" + item.getBillId());
                }
                settle(bill, item.getAmount(), item.getAmountFc());
            }
        }
        settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate()
                .set(FinSettlement::getStatus, approved ? STATUS_APPROVED : STATUS_REJECTED)
                .eq(FinSettlement::getId, s.getId())
                .eq(FinSettlement::getStatus, STATUS_APPROVING));
    }

    /** 核销：本位币按单据记账汇率（原币核销完时取剩余本位币，避免尾差），同时累加原币已核销 */
    private void settle(FinBill bill, BigDecimal base, BigDecimal fc) {
        if (billMapper.addSettled(bill.getId(), base) == 0) {
            FinBill latest = billMapper.selectById(bill.getId());
            BigDecimal remaining = latest == null ? BigDecimal.ZERO : latest.getAmount().subtract(latest.getSettledAmount());
            throw new BizException(bill.getBillNo() + "：核销金额超过未核销金额 " + remaining.toPlainString());
        }
        billMapper.addSettledFc(bill.getId(), fc == null ? base : fc);
    }

    /** 原币核销金额对应的记账本位币：本位币单据即为本身；外币按记账汇率，核销完剩余原币时取剩余本位币 */
    private static BigDecimal baseOf(FinBill bill, BigDecimal fc) {
        if (bill.getCurrency() == null || "CNY".equals(bill.getCurrency())) {
            return fc;
        }
        BigDecimal remainingFc = bill.getAmountFc().subtract(bill.getSettledFc());
        if (fc.compareTo(remainingFc) == 0) {
            return bill.getAmount().subtract(bill.getSettledAmount());
        }
        return fc.multiply(bill.getExchangeRate()).setScale(2, java.math.RoundingMode.HALF_UP);
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
            if (!forNote && SettleMethod.NOTE.name().equals(s.getMethod())) {
                throw new BizException(s.getSettleNo() + " 由票据业务生成，请在「票据管理」中删除票据");
            }
            if (s.getVoucherId() != null) {
                throw new BizException(s.getSettleNo() + " 已生成凭证，请先删除凭证");
            }
            if (STATUS_APPROVING.equals(s.getStatus())) {
                throw new BizException(s.getSettleNo() + " 审批中，请先在「我的申请」中撤回");
            }
            periodGuard.checkOpen(s.getSettleDate(), "收付款单");
            // 已拒绝的付款没有核销过，不需要恢复
            if (STATUS_APPROVED.equals(s.getStatus())) {
                for (FinSettlementItem item : itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery()
                        .eq(FinSettlementItem::getSettlementId, id))) {
                    billMapper.addSettled(item.getBillId(), item.getAmount().negate());
                    billMapper.addSettledFc(item.getBillId(), (item.getAmountFc() == null ? item.getAmount() : item.getAmountFc()).negate());
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
            if (s.getVoucherId() != null || !STATUS_APPROVED.equals(s.getStatus())) {
                continue;
            }
            boolean receipt = "RECEIPT".equals(s.getKind());
            String summary = (receipt ? "收到 " : "支付 ") + billService.partnerName(s.getKind(), s.getPartnerId()) + " 货款";
            Long partnerAccount = accountService.leafByCode(receipt ? RECEIVABLE : PAYABLE).getId();
            // 往来科目按核销的记账本位币，银行按实收实付本位币，外币差额计入财务费用（汇兑损益）
            BigDecimal booked = itemMapper.selectList(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getSettlementId, s.getId()))
                    .stream().map(FinSettlementItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<VoucherLine> lines = new java.util.ArrayList<>(receipt
                    ? List.of(VoucherLine.debit(summary, s.getAccountId(), null, s.getAmount()),
                    VoucherLine.credit(summary, partnerAccount, null, booked).withCustomer(s.getPartnerId()))
                    : List.of(VoucherLine.debit(summary, partnerAccount, null, booked).withSupplier(s.getPartnerId()),
                    VoucherLine.credit(summary, s.getAccountId(), null, s.getAmount())));
            BigDecimal diff = receipt ? s.getAmount().subtract(booked) : booked.subtract(s.getAmount());
            if (diff.signum() != 0) {
                // 收款多收 / 付款少付为汇兑收益，冲减财务费用
                lines.add(VoucherLine.credit(summary + " 汇兑损益", accountService.leafByCode(FINANCE_EXPENSE).getId(), null, diff));
            }
            Long voucherId = voucherService.createGenerated(s.getSettleDate(), receipt ? VoucherSource.RECEIPT : VoucherSource.PAYMENT,
                    s.getSettleNo(), lines);
            settlementMapper.update(Wrappers.<FinSettlement>lambdaUpdate().set(FinSettlement::getVoucherId, voucherId).eq(FinSettlement::getId, s.getId()));
            count++;
        }
        return count;
    }

    private static void checkKind(String kind) {
        if (!"RECEIPT".equals(kind) && !"PAYMENT".equals(kind)) {
            throw new BizException("收付款种类不正确：" + kind);
        }
    }
}
