package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AgingRow;
import com.lemon.erp.modules.fin.dto.BillQuery;
import com.lemon.erp.modules.fin.dto.BillRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinSettlementItem;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementItemMapper;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.mes.entity.QmsSupplierClaim;
import com.lemon.erp.modules.mes.event.SupplierClaimCancelledEvent;
import com.lemon.erp.modules.mes.event.SupplierClaimConfirmedEvent;
import com.lemon.erp.modules.mes.mapper.QmsSupplierClaimMapper;
import com.lemon.erp.modules.scm.entity.ScmRebate;
import com.lemon.erp.modules.scm.event.RebateSettledEvent;
import com.lemon.erp.modules.scm.event.RebateUnsettledEvent;
import com.lemon.erp.modules.scm.mapper.ScmRebateMapper;
import com.lemon.erp.modules.scm.model.ScmConstants;
import com.lemon.erp.modules.scm.entity.ScmShipment;
import com.lemon.erp.modules.scm.event.ShipmentShippedEvent;
import com.lemon.erp.modules.scm.event.TradeDocConfirmedEvent;
import com.lemon.erp.modules.scm.mapper.ScmShipmentMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lemon.erp.modules.fin.model.FinConstants.BILL_AP;
import static com.lemon.erp.modules.fin.model.FinConstants.BILL_AR;
import static com.lemon.erp.modules.fin.model.FinConstants.BILL_SOURCE_PROJECT;
import static com.lemon.erp.modules.fin.model.FinConstants.BILL_SOURCE_QUALITY_CLAIM;
import static com.lemon.erp.modules.fin.model.FinConstants.BILL_SOURCE_TICKET;
import static com.lemon.erp.modules.fin.model.FinConstants.BILL_SOURCE_REBATE;
import static com.lemon.erp.modules.fin.model.FinConstants.INPUT_VAT;
import static com.lemon.erp.modules.fin.model.FinConstants.OUTPUT_VAT;

/**
 * 应收 / 应付单：
 * <ul>
 *   <li>监听供应链的出入库确认事件，销售出库 / 退货生成应收单，采购入库 / 退货生成应付单（退货为红字负数），到期日按往来单位账期</li>
 *   <li>手工录入期初余额等</li>
 *   <li>生成凭证：应收 借 应收账款 / 贷 主营业务收入、销项税额；应付 借 原材料 / 库存商品（按物料分类）、进项税额 / 贷 应付账款</li>
 *   <li>账龄分析：未核销余额按逾期天数分段</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FinBillService {

    private static final Set<String> KINDS = Set.of("AR", "AP");
    private static final BigDecimal REBATE_TAX_RATE = BigDecimal.valueOf(13);

    private final FinBillMapper billMapper;
    private final FinSettlementItemMapper settlementItemMapper;
    private final FinVoucherService voucherService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final ScmCustomerService customerService;
    private final ScmSupplierService supplierService;
    private final DocNoService docNoService;
    private final FinBizPeriodGuard periodGuard;
    private final ScmShipmentMapper shipmentMapper;
    private final QmsSupplierClaimMapper claimMapper;
    private final ScmRebateMapper rebateMapper;
    private final ScmQueryService scmQueryService;

    public PageResult<FinBill> page(long current, long size, BillQuery query) {
        checkKind(query.kind());
        Page<FinBill> page = Page.of(current, size);
        billMapper.selectPageList(page, query, LocalDate.now());
        return PageResult.of(page);
    }

    /** 往来单位未核销的单据，收付款时选择核销 */
    public List<FinBill> unsettled(String kind, Long partnerId) {
        checkKind(kind);
        return billMapper.selectList(Wrappers.<FinBill>lambdaQuery()
                .eq(FinBill::getKind, kind)
                .eq(FinBill::getPartnerId, partnerId)
                .ne(FinBill::getStatus, "SETTLED")
                .orderByAsc(FinBill::getDueDate).orderByAsc(FinBill::getId));
    }

    public PageResult<AgingRow> aging(long current, long size, String kind, LocalDate asOf) {
        checkKind(kind);
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        Page<AgingRow> page = Page.of(current, size);
        billMapper.selectAging(page, kind, date, date.minusDays(30), date.minusDays(60), date.minusDays(90));
        return PageResult.of(page);
    }

    /** 手工录入（期初余额等），不生成凭证 */
    @Transactional
    public Long create(BillRequest req) {
        checkKind(req.kind());
        if (req.amount().signum() == 0) {
            throw new BizException("金额不能为 0");
        }
        int paymentDays = paymentDays(req.kind(), req.partnerId());
        FinBill bill = newBill(req.kind(), req.partnerId(), "MANUAL", null, null, req.billDate(),
                req.dueDate() != null ? req.dueDate() : req.billDate().plusDays(paymentDays), req.amount(), BigDecimal.ZERO);
        bill.setRemark(req.remark());
        billMapper.insert(bill);
        return bill.getId();
    }

    /**
     * 其他模块的业务单据生成应收 / 应付单（如项目开票），可以生成凭证（借 应收账款，贷 合同负债、销项税额）
     *
     * @param sourceType 来源（字典 fin_bill_source），如 PROJECT
     * @param projectId  项目（辅助核算），可为空
     * @return 应收 / 应付单
     */
    @Transactional
    public FinBill createFromSource(String kind, Long partnerId, String sourceType, Long sourceId, String sourceNo,
                                    LocalDate billDate, BigDecimal amount, BigDecimal tax, String remark, Long projectId) {
        checkKind(kind);
        if (amount.signum() == 0) {
            throw new BizException("金额不能为 0");
        }
        periodGuard.checkOpen(billDate, "AR".equals(kind) ? "应收单" : "应付单"); // i18n-ignore
        FinBill bill = newBill(kind, partnerId, sourceType, sourceId, sourceNo, billDate,
                billDate.plusDays(paymentDays(kind, partnerId)), amount, tax);
        bill.setRemark(remark);
        bill.setProjectId(projectId);
        billMapper.insert(bill);
        return bill;
    }

    /** 删除其他模块生成的单据（如取消项目开票）：未核销、未生成凭证 */
    @Transactional
    public void deleteSourceBill(Long id) {
        FinBill bill = billMapper.selectById(id);
        if (bill == null) {
            return;
        }
        if (bill.getSettledAmount().signum() != 0
                || settlementItemMapper.exists(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getBillId, id))) {
            throw new BizException("{0} 已收款核销，不能取消", bill.getBillNo());
        }
        if (bill.getVoucherId() != null) {
            throw new BizException("{0} 已生成凭证，请先在财务中删除凭证", bill.getBillNo());
        }
        periodGuard.checkOpen(bill.getBillDate(), "应收应付单"); // i18n-ignore
        billMapper.deleteById(id);
    }

    /** 只能删除手工录入、未核销、未生成凭证的单据 */
    @Transactional
    public void delete(List<Long> ids) {
        for (FinBill bill : billMapper.selectByIds(ids)) {
            if (!"MANUAL".equals(bill.getSourceType())) {
                throw new BizException("{0} 由业务单据生成，不能删除", bill.getBillNo());
            }
            if (bill.getSettledAmount().signum() != 0
                    || settlementItemMapper.exists(Wrappers.<FinSettlementItem>lambdaQuery().eq(FinSettlementItem::getBillId, bill.getId()))) {
                throw new BizException("{0} 已核销，不能删除", bill.getBillNo());
            }
        }
        billMapper.deleteByIds(ids);
    }

    /** 发运单发货：运费生成承运商应付单（借 销售费用），与发货在同一事务 */
    @EventListener
    public void onShipmentShipped(ShipmentShippedEvent e) {
        FinBill bill = newBill("AP", e.carrierId(), "SHIPMENT", e.shipmentId(), e.shipNo(), e.shipDate(),
                e.shipDate().plusDays(paymentDays("AP", e.carrierId())), e.freight(), BigDecimal.ZERO);
        bill.setExpenseAccountId(accountRules.accountId(AccountRole.SELLING_EXPENSE));
        bill.setDeptId(e.deptId());
        bill.setRemark("发运单 " + e.shipNo() + " 运费"); // i18n-ignore
        billMapper.insert(bill);
        shipmentMapper.update(Wrappers.<ScmShipment>lambdaUpdate().set(ScmShipment::getFreightBillId, bill.getId())
                .eq(ScmShipment::getId, e.shipmentId()));
    }

    /** 供应商质量索赔确认：生成红字应付单冲减应付，与确认同一事务 */
    @EventListener
    public void onSupplierClaimConfirmed(SupplierClaimConfirmedEvent e) {
        FinBill bill = createFromSource(BILL_AP, e.supplierId(), BILL_SOURCE_QUALITY_CLAIM, e.claimId(), e.claimNo(), e.claimDate(),
                e.amount().negate(), BigDecimal.ZERO, "质量索赔 " + e.claimNo() + "：" + e.reason(), null); // i18n-ignore
        claimMapper.update(Wrappers.<QmsSupplierClaim>lambdaUpdate().set(QmsSupplierClaim::getBillId, bill.getId())
                .eq(QmsSupplierClaim::getId, e.claimId()));
    }

    /** 已确认的质量索赔取消：删除红字应付单（已核销、已生成凭证时抛异常，取消回滚） */
    @EventListener
    public void onSupplierClaimCancelled(SupplierClaimCancelledEvent e) {
        if (e.billId() != null) {
            deleteSourceBill(e.billId());
        }
    }

    /** 销售返利结算（红字应收方式）：生成红字应收单冲减应收，税额按 13% 价内计算；与结算同一事务 */
    @EventListener
    public void onRebateSettled(RebateSettledEvent e) {
        BigDecimal tax = ScmConstants.tax(e.amount(), REBATE_TAX_RATE);
        FinBill bill = createFromSource(BILL_AR, e.customerId(), BILL_SOURCE_REBATE, e.rebateId(), e.rebateNo(), e.settleDate(),
                e.amount().negate(), tax.negate(), "销售返利 " + e.rebateNo(), null); // i18n-ignore
        rebateMapper.update(Wrappers.<ScmRebate>lambdaUpdate().set(ScmRebate::getBillId, bill.getId()).eq(ScmRebate::getId, e.rebateId()));
    }

    /** 撤销返利结算：删除红字应收单（已核销、已生成凭证时抛异常，撤销回滚） */
    @EventListener
    public void onRebateUnsettled(RebateUnsettledEvent e) {
        deleteSourceBill(e.billId());
    }

    /** 出入库确认后生成应收 / 应付单，与出入库在同一事务 */
    @EventListener
    public void onTradeDocConfirmed(TradeDocConfirmedEvent e) {
        TradeKind kind = TradeKind.valueOf(e.kind());
        String billKind = kind.getOrderKind() == OrderKind.SALES ? BILL_AR : BILL_AP;
        // 采购订单引用了约定付款天数的采购合同时按合同计算到期日
        Integer contractDays = kind.getOrderKind() == OrderKind.PURCHASE ? scmQueryService.contractPaymentDays(e.docId()) : null;
        LocalDate due = e.bizDate().plusDays(contractDays != null ? contractDays : paymentDays(billKind, e.partnerId()));
        FinBill bill = newBill(billKind, e.partnerId(), kind.name(), e.docId(), e.docNo(), e.bizDate(), due, e.totalAmount(), e.taxAmount());
        if (e.currency() != null && !"CNY".equals(e.currency())) {
            bill.setCurrency(e.currency());
            bill.setExchangeRate(e.exchangeRate());
            bill.setAmountFc(e.totalAmountFc());
        }
        billMapper.insert(bill);
    }

    /**
     * 为业务生成的单据逐张生成凭证（未审核），已生成的跳过
     *
     * @return 生成的凭证数
     */
    @Transactional
    public int generateVouchers(List<Long> ids) {
        int count = 0;
        for (FinBill bill : billMapper.selectByIds(ids)) {
            if (bill.getVoucherId() != null) {
                continue;
            }
            if ("MANUAL".equals(bill.getSourceType())) {
                throw new BizException("{0} 为手工录入的期初单据，不需要生成凭证", bill.getBillNo());
            }
            Long voucherId = voucherService.createGenerated(bill.getBillDate(),
                    "AR".equals(bill.getKind()) ? VoucherSource.AR : VoucherSource.AP, bill.getBillNo(), lines(bill));
            billMapper.update(Wrappers.<FinBill>lambdaUpdate().set(FinBill::getVoucherId, voucherId).eq(FinBill::getId, bill.getId()));
            count++;
        }
        return count;
    }

    /**
     * 坏账核销（直接转销法）：确认无法收回的应收款，计入已核销，生成凭证 借 营业外支出 / 贷 应收账款
     *
     * @return 凭证 ID
     */
    @Transactional
    public Long writeOffBadDebt(Long id, BigDecimal amount, LocalDate date, String reason) {
        FinBill bill = billMapper.selectById(id);
        if (bill == null || !"AR".equals(bill.getKind())) {
            throw new BizException("应收单不存在");
        }
        if (bill.getAmount().signum() <= 0 || amount == null || amount.signum() <= 0) {
            throw new BizException("只能核销红字以外的应收单，金额必须大于 0");
        }
        if (bill.getVoucherId() == null && !"MANUAL".equals(bill.getSourceType())) {
            throw new BizException("{0} 还没有生成应收凭证，请先生成凭证", bill.getBillNo());
        }
        if (bill.getCurrency() != null && !"CNY".equals(bill.getCurrency())) {
            throw new BizException("外币应收单请先收款或期末调汇后再处理坏账");
        }
        LocalDate d = date == null ? LocalDate.now() : date;
        periodGuard.checkOpen(d, "坏账核销"); // i18n-ignore
        if (billMapper.addSettled(id, amount) == 0) {
            throw new BizException("核销金额超过未核销金额 {0}", bill.getAmount().subtract(bill.getSettledAmount()).toPlainString());
        }
        billMapper.addSettledFc(id, amount);
        billMapper.addBadDebt(id, amount);
        String summary = "坏账核销 " + bill.getBillNo() + " " + partnerName("AR", bill.getPartnerId()) // i18n-ignore
                + (reason == null || reason.isBlank() ? "" : " " + reason);
        return voucherService.createGenerated(d, VoucherSource.BAD_DEBT, bill.getBillNo(), List.of(
                VoucherLine.debit(summary, accountRules.accountId(AccountRole.NON_OPERATING_EXPENSE), null, amount),
                VoucherLine.credit(summary, accountRules.accountId(AccountRole.RECEIVABLE), null, amount)
                        .withCustomer(bill.getPartnerId()).withProject(bill.getProjectId())));
    }

    // ---------------------------------------------------------------- 内部

    /** 凭证分录：退货单据金额为负数，VoucherLine 自动记到相反方向 */
    private List<VoucherLine> lines(FinBill bill) {
        String partner = partnerName(bill.getKind(), bill.getPartnerId());
        List<VoucherLine> lines = new ArrayList<>();
        BigDecimal sign = BigDecimal.valueOf(bill.getAmount().signum() < 0 ? -1 : 1);
        if ("AR".equals(bill.getKind())) {
            String summary = (BILL_SOURCE_PROJECT.equals(bill.getSourceType()) ? "项目开票 " : BILL_SOURCE_TICKET.equals(bill.getSourceType()) ? "服务收费 " // i18n-ignore
                    : BILL_SOURCE_REBATE.equals(bill.getSourceType()) ? "销售返利 " : sign.signum() < 0 ? "销售退货 " : "销售出库 ") // i18n-ignore
                    + bill.getSourceNo() + " " + partner;
            BigDecimal net = bill.getAmount().subtract(bill.getTaxAmount());
            boolean project = BILL_SOURCE_PROJECT.equals(bill.getSourceType());
            lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.RECEIVABLE), null, bill.getAmount())
                    .withCustomer(bill.getPartnerId()).withProject(bill.getProjectId()));
            if (isTradeDoc(bill)) {
                // 销售出库 / 退货：主营业务收入按物料分类取科目
                revenueByCategory(bill, net).forEach((category, amount) -> lines.add(
                        VoucherLine.credit(summary, accountRules.accountId(AccountRole.REVENUE, category), null, amount)));
            } else {
                // 项目开票计入合同负债，按完工百分比确认收入时再转入主营业务收入（项目月结）
                // 服务工单收费计入其他业务收入
                AccountRole revenue = project ? AccountRole.CONTRACT_LIABILITY
                        : BILL_SOURCE_TICKET.equals(bill.getSourceType()) ? AccountRole.OTHER_REVENUE : AccountRole.REVENUE;
                lines.add(VoucherLine.credit(summary, accountRules.accountId(revenue), null, net)
                        .withProject(bill.getProjectId()));
            }
            lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.OUTPUT_VAT), null, bill.getTaxAmount()));
            return lines;
        }
        if (BILL_SOURCE_QUALITY_CLAIM.equals(bill.getSourceType())) {
            // 质量索赔（红字应付单）：借 应付账款 / 贷 营业外收入
            String summary = "质量索赔 " + bill.getSourceNo() + " " + partner; // i18n-ignore
            BigDecimal amount = bill.getAmount().negate();
            lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.PAYABLE), null, amount)
                    .withSupplier(bill.getPartnerId()));
            lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.NON_OPERATING_INCOME), null, amount));
            return lines;
        }
        if (bill.getExpenseAccountId() != null) {
            // 费用类应付（运费等）：借 费用科目、进项税额 / 贷 应付账款
            String summary = "SHIPMENT".equals(bill.getSourceType()) ? "运费 " + bill.getSourceNo() + " " + partner // i18n-ignore
                    : bill.getSourceNo() + " " + partner;
            lines.add(VoucherLine.debit(summary, bill.getExpenseAccountId(), bill.getDeptId(), bill.getAmount().subtract(bill.getTaxAmount())));
            lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.INPUT_VAT), null, bill.getTaxAmount()));
            lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.PAYABLE), null, bill.getAmount())
                    .withSupplier(bill.getPartnerId()));
            return lines;
        }
        String summary = (sign.signum() < 0 ? "采购退货 " : "采购入库 ") + bill.getSourceNo() + " " + partner; // i18n-ignore
        for (Map<String, Object> row : billMapper.selectSourceByCategory(bill.getSourceId())) {
            BigDecimal net = new BigDecimal(row.get("net").toString()).multiply(sign);
            lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.INVENTORY, (String) row.get("category")), null, net));
        }
        lines.add(VoucherLine.debit(summary, accountRules.accountId(AccountRole.INPUT_VAT), null, bill.getTaxAmount()));
        lines.add(VoucherLine.credit(summary, accountRules.accountId(AccountRole.PAYABLE), null, bill.getAmount())
                .withSupplier(bill.getPartnerId()));
        return lines;
    }

    private static boolean isTradeDoc(FinBill bill) {
        return Arrays.stream(TradeKind.values()).anyMatch(k -> k.name().equals(bill.getSourceType()));
    }

    /** 不含税收入按出库单各物料分类的金额比例分摊，尾差计入最后一类 */
    private Map<String, BigDecimal> revenueByCategory(FinBill bill, BigDecimal net) {
        List<Map<String, Object>> rows = billMapper.selectSourceByCategory(bill.getSourceId());
        BigDecimal total = rows.stream().map(r -> new BigDecimal(r.get("net").toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (rows.isEmpty() || total.signum() == 0) {
            result.put(null, net);
            return result;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            String category = (String) rows.get(i).get("category");
            BigDecimal amount = i == rows.size() - 1 ? net.subtract(allocated)
                    : net.multiply(new BigDecimal(rows.get(i).get("net").toString())).divide(total, 2, RoundingMode.HALF_UP);
            result.merge(category, amount, BigDecimal::add);
            allocated = allocated.add(amount);
        }
        return result;
    }

    private FinBill newBill(String kind, Long partnerId, String sourceType, Long sourceId, String sourceNo, LocalDate billDate,
                            LocalDate dueDate, BigDecimal amount, BigDecimal tax) {
        FinBill bill = new FinBill();
        bill.setBillNo(docNoService.next("AR".equals(kind) ? "YS" : "YF"));
        bill.setKind(kind);
        bill.setPartnerId(partnerId);
        bill.setSourceType(sourceType);
        bill.setSourceId(sourceId);
        bill.setSourceNo(sourceNo);
        bill.setBillDate(billDate);
        bill.setDueDate(dueDate);
        bill.setAmount(amount);
        bill.setTaxAmount(tax);
        bill.setSettledAmount(BigDecimal.ZERO);
        bill.setInvoicedAmount(BigDecimal.ZERO);
        bill.setBadDebtAmount(BigDecimal.ZERO);
        bill.setCurrency("CNY");
        bill.setExchangeRate(BigDecimal.ONE);
        bill.setAmountFc(amount);
        bill.setSettledFc(BigDecimal.ZERO);
        bill.setStatus("UNSETTLED");
        bill.setCreateBy(SecurityUtils.getUserId());
        return bill;
    }

    private int paymentDays(String kind, Long partnerId) {
        Integer days = "AR".equals(kind) ? customerService.get(partnerId).getPaymentDays() : supplierService.get(partnerId).getPaymentDays();
        return days == null ? 0 : days;
    }

    String partnerName(String kind, Long partnerId) {
        return FinConstants.BILL_AR.equals(kind) || SettleKind.RECEIPT.name().equals(kind) ? customerService.get(partnerId).getName() : supplierService.get(partnerId).getName();
    }

    static void checkKind(String kind) {
        if (!KINDS.contains(kind)) {
            throw new BizException("单据种类不正确：{0}", kind);
        }
    }
}
