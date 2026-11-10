package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.RebateRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmRebate;
import com.lemon.erp.modules.scm.entity.ScmRebateItem;
import com.lemon.erp.modules.scm.entity.ScmRebateTier;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.event.RebateSettledEvent;
import com.lemon.erp.modules.scm.event.RebateUnsettledEvent;
import com.lemon.erp.modules.scm.mapper.ScmRebateMapper;
import com.lemon.erp.modules.scm.model.RebateCalcMode;
import com.lemon.erp.modules.scm.model.RebateKind;
import com.lemon.erp.modules.scm.model.RebateSettle;
import com.lemon.erp.modules.scm.model.RebateStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * 返利协议：销售返利（客户 × 期间）、采购返利（供应商 × 期间，供应商给我方的返利）。
 * 计算方式：阶梯比例（期间金额 = 销售出库 - 销售退货 / 采购入库 - 采购退货，含税，本位币；返利 = 金额 × 达到的最高一档返利率），
 * 或按项目填写金额（逐项填写返利金额，结算时取合计，期间金额只作参考）。期间结束后结算：
 * 销售返利的红字应收方式由财务生成红字应收单冲减应收（借 主营业务收入、销项税额 / 贷 应收账款），下次订单抵扣方式计入客户的返利余额；
 * 采购返利只能红字应付，由财务生成红字应付单（借 应付账款 / 贷 采购返利科目、进项税额），付款时勾选即可与欠款冲抵
 */
@Service
@RequiredArgsConstructor
public class ScmRebateService {

    /** 销售返利、采购返利协议号的编码规则标识 */
    public static final String DOC_NO = "FL";
    public static final String PURCHASE_DOC_NO = "CFL";

    private static final String BRAND_DICT = "wms_material_brand";
    private static final String CATEGORY_DICT = "wms_material_category";

    private final ScmRebateMapper rebateMapper;
    private final ScmCustomerService customerService;
    private final ScmSupplierService supplierService;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;
    private final ApplicationEventPublisher eventPublisher;

    /** 计算结果：期间金额（销售额 / 采购额）、返利率（按项目填写时为空）、返利金额 */
    public record Calc(BigDecimal salesAmount, BigDecimal rate, BigDecimal rebateAmount) {
    }

    public PageResult<ScmRebate> page(RebateKind kind, long current, long size, Long partnerId, RebateStatus status, String keyword) {
        Page<ScmRebate> page = Page.of(current, size);
        rebateMapper.selectPageList(page, kind, partnerId, status, keyword);
        return PageResult.of(page);
    }

    /** 协议详情；种类不符时视为不存在（销售返利、采购返利分别授权） */
    public ScmRebate detail(RebateKind kind, Long id) {
        ScmRebate r = id == null ? null : rebateMapper.selectDetail(id);
        if (r == null || r.getKind() != kind) {
            throw new BizException("scm.error.rebateAgreementDoesNotExist");
        }
        r.setTiers(rebateMapper.selectTiers(id));
        r.setItems(rebateMapper.selectItems(id));
        return r;
    }

    @Transactional
    public Long create(RebateKind kind, RebateRequest req) {
        validate(kind, req);
        ScmRebate r = new ScmRebate();
        r.setRebateNo(docNoService.next(kind == RebateKind.PURCHASE ? PURCHASE_DOC_NO : DOC_NO));
        r.setKind(kind);
        fill(kind, r, req);
        r.setStatus(RebateStatus.ACTIVE);
        rebateMapper.insert(r);
        saveLines(r.getId(), req);
        return r.getId();
    }

    @Transactional
    public void update(RebateKind kind, Long id, RebateRequest req) {
        ScmRebate r = requireActive(kind, id);
        validate(kind, req);
        fill(kind, r, req);
        rebateMapper.updateById(r);
        rebateMapper.deleteTiers(id);
        rebateMapper.deleteItems(id);
        saveLines(id, req);
    }

    @Transactional
    public void delete(RebateKind kind, Long id) {
        requireActive(kind, id);
        rebateMapper.deleteTiers(id);
        rebateMapper.deleteItems(id);
        rebateMapper.deleteById(id);
    }

    /** 按当前数据计算（期间未结束时为截至今天的预估） */
    public Calc calc(RebateKind kind, Long id) {
        ScmRebate r = detail(kind, id);
        LocalDate end = r.getEndDate().isAfter(LocalDate.now()) ? LocalDate.now() : r.getEndDate();
        return calc(r, end);
    }

    /** 结算：期间结束后计算返利；红字应收 / 应付方式由财务生成红字单据 */
    @Transactional
    public Calc settle(RebateKind kind, Long id, LocalDate settleDate) {
        ScmRebate r = requireActive(kind, id);
        if (!r.getEndDate().isBefore(LocalDate.now())) {
            throw new BizException("scm.error.rebatePeriodHasNotEndedCannot", r.getEndDate());
        }
        Calc c = calc(r, r.getEndDate());
        if (c.rebateAmount().signum() <= 0) {
            throw new BizException("scm.error.periodSalesAmountDoesNotReach", plain(c.salesAmount()));
        }
        LocalDate date = settleDate != null ? settleDate : LocalDate.now();
        if (!StatusTransitions.tryChange(rebateMapper, ScmRebate::getId, id, ScmRebate::getStatus, RebateStatus.ACTIVE, RebateStatus.SETTLED,
                w -> w.set(ScmRebate::getSalesAmount, c.salesAmount())
                        .set(ScmRebate::getRate, c.rate()).set(ScmRebate::getRebateAmount, c.rebateAmount())
                        .set(ScmRebate::getSettleDate, date).set(ScmRebate::getSettleBy, SecurityUtils.getUserId())
                        .set(ScmRebate::getUpdateTime, LocalDateTime.now()))) {
            throw new BizException("scm.error.rebateAgreementStatusHasChangedRefresh");
        }
        if (r.getSettleMode() == RebateSettle.CREDIT_NOTE) {
            eventPublisher.publishEvent(new RebateSettledEvent(id, r.getRebateNo(), kind, partnerId(r), c.rebateAmount(), date));
        }
        return c;
    }

    /** 撤销结算：红字应收 / 应付单须未核销、未生成凭证；下次订单抵扣的须还没有被使用（余额足够） */
    @Transactional
    public void unsettle(RebateKind kind, Long id) {
        ScmRebate r = detail(kind, id);
        if (r.getStatus() != RebateStatus.SETTLED) {
            throw new BizException("scm.error.rebateAgreementHasNotSettled");
        }
        if (r.getSettleMode() == RebateSettle.NEXT_ORDER && balance(r.getCustomerId(), null).compareTo(r.getRebateAmount()) < 0) {
            throw new BizException("scm.error.rebateHasDeductedSalesOrdersSettlement");
        }
        rebateMapper.update(Wrappers.<ScmRebate>lambdaUpdate()
                .set(ScmRebate::getStatus, RebateStatus.ACTIVE).set(ScmRebate::getSalesAmount, null).set(ScmRebate::getRate, null)
                .set(ScmRebate::getRebateAmount, null).set(ScmRebate::getSettleDate, null).set(ScmRebate::getSettleBy, null)
                .set(ScmRebate::getBillId, null).set(ScmRebate::getUpdateTime, LocalDateTime.now())
                .eq(ScmRebate::getId, id));
        if (r.getBillId() != null) {
            eventPublisher.publishEvent(new RebateUnsettledEvent(id, r.getBillId()));
        }
    }

    /**
     * 记录返利结算（红字应收 / 应付方式）生成的红字单据。由财务模块在处理 {@link RebateSettledEvent} 时调用，与结算在同一事务
     */
    public void bindBill(Long rebateId, Long billId) {
        rebateMapper.update(Wrappers.<ScmRebate>lambdaUpdate().set(ScmRebate::getBillId, billId).eq(ScmRebate::getId, rebateId));
    }

    /**
     * 客户可用的返利余额：已结算的「下次订单抵扣」销售返利 - 销售订单已抵扣
     *
     * @param excludeOrderId 修改订单时排除该订单已抵扣的金额
     */
    public BigDecimal balance(Long customerId, Long excludeOrderId) {
        return rebateMapper.selectEarned(customerId).subtract(rebateMapper.selectUsed(customerId, excludeOrderId));
    }

    /** 销售订单抵扣返利前校验余额 */
    public void checkBalance(Long customerId, BigDecimal amount, Long excludeOrderId) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        BigDecimal balance = balance(customerId, excludeOrderId);
        if (amount.compareTo(balance) > 0) {
            throw new BizException("scm.error.rebateDeductionExceedsCustomerSRebate", plain(amount), plain(balance));
        }
    }

    private Calc calc(ScmRebate r, LocalDate end) {
        BigDecimal amount = rebateMapper.selectTradeAmount(r.getKind(), partnerId(r), r.getBeginDate(), end);
        if (r.getCalcMode() == RebateCalcMode.ITEM) {
            return new Calc(amount, null, Decimals.sum(r.getItems(), ScmRebateItem::getAmount));
        }
        BigDecimal rate = r.getTiers().stream().filter(t -> amount.compareTo(t.getMinAmount()) >= 0)
                .max(Comparator.comparing(ScmRebateTier::getMinAmount)).map(ScmRebateTier::getRate).orElse(BigDecimal.ZERO);
        return new Calc(amount, rate, amount.multiply(rate).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP));
    }

    private static Long partnerId(ScmRebate r) {
        return r.getKind() == RebateKind.PURCHASE ? r.getSupplierId() : r.getCustomerId();
    }

    private ScmRebate requireActive(RebateKind kind, Long id) {
        ScmRebate r = detail(kind, id);
        if (r.getStatus() != RebateStatus.ACTIVE) {
            throw new BizException("scm.error.rebateAgreementHasSettledCannotModified");
        }
        return r;
    }

    private void validate(RebateKind kind, RebateRequest req) {
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("scm.error.endDateCannotEarlierStartDate");
        }
        if (calcMode(req) == RebateCalcMode.ITEM) {
            if (req.items() == null || req.items().isEmpty()) {
                throw new BizException("scm.error.addRebateItems");
            }
            for (RebateRequest.Item i : req.items()) {
                dictLookup.check(BRAND_DICT, i.brand(), "scm.label.brand");
                dictLookup.check(CATEGORY_DICT, i.category(), "scm.label.materialCategory");
            }
        } else {
            if (req.tiers() == null || req.tiers().isEmpty()) {
                throw new BizException("scm.error.addRebateTiers");
            }
            if (req.tiers().stream().map(t -> t.minAmount().stripTrailingZeros()).collect(HashSet::new, HashSet::add, HashSet::addAll).size()
                    != req.tiers().size()) {
                throw new BizException("scm.error.rebateTierSalesAmountsCannotDuplicated");
            }
        }
        if (kind == RebateKind.PURCHASE) {
            // 采购返利冲减应付（红字应付单），不支持在采购订单中抵扣
            if (req.settleMode() != RebateSettle.CREDIT_NOTE) {
                throw new BizException("scm.error.purchaseRebateCreditNoteOnly");
            }
            if (req.supplierId() == null) {
                throw new BizException("scm.error.selectRebateSupplier");
            }
            ScmSupplier supplier = supplierService.get(req.supplierId());
            if (supplier.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("scm.error.supplierDisabled", supplier.getName());
            }
        } else {
            if (req.customerId() == null) {
                throw new BizException("scm.error.selectRebateCustomer");
            }
            ScmCustomer customer = customerService.get(req.customerId());
            if (customer.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("scm.error.customerDisabled", customer.getName());
            }
        }
    }

    private static RebateCalcMode calcMode(RebateRequest req) {
        return req.calcMode() == null ? RebateCalcMode.TIER : req.calcMode();
    }

    private static void fill(RebateKind kind, ScmRebate r, RebateRequest req) {
        r.setCustomerId(kind == RebateKind.SALES ? req.customerId() : null);
        r.setSupplierId(kind == RebateKind.PURCHASE ? req.supplierId() : null);
        r.setCalcMode(calcMode(req));
        r.setBeginDate(req.beginDate());
        r.setEndDate(req.endDate());
        r.setSettleMode(req.settleMode());
        r.setRemark(req.remark());
    }

    /** 按计算方式保存阶梯或返利项目（另一种不保存） */
    private void saveLines(Long rebateId, RebateRequest req) {
        if (calcMode(req) == RebateCalcMode.ITEM) {
            rebateMapper.insertItems(rebateId, req.items().stream().map(i -> {
                ScmRebateItem item = new ScmRebateItem();
                item.setItemName(i.itemName().trim());
                item.setBrand(blankToNull(i.brand()));
                item.setCategory(blankToNull(i.category()));
                item.setAmount(i.amount());
                item.setRemark(blankToNull(i.remark()));
                return item;
            }).toList());
        } else {
            rebateMapper.insertTiers(rebateId, req.tiers().stream().map(t -> {
                ScmRebateTier tier = new ScmRebateTier();
                tier.setMinAmount(t.minAmount());
                tier.setRate(t.rate());
                return tier;
            }).toList());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
