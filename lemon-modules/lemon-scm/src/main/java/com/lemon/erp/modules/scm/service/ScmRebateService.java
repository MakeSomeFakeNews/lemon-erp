package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.RebateRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmRebate;
import com.lemon.erp.modules.scm.entity.ScmRebateTier;
import com.lemon.erp.modules.scm.event.RebateSettledEvent;
import com.lemon.erp.modules.scm.event.RebateUnsettledEvent;
import com.lemon.erp.modules.scm.mapper.ScmRebateMapper;
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
 * 销售返利：客户 × 期间的阶梯返利协议。期间结束后结算：销售额 = 期间内销售出库 - 销售退货（含税，本位币），
 * 返利 = 销售额 × 达到的最高一档返利率。红字应收方式由财务生成红字应收单冲减应收（借 主营业务收入、销项税额 / 贷 应收账款）；
 * 下次订单抵扣方式计入客户的返利余额，新建销售订单时抵扣（按行金额分摊到单价）
 */
@Service
@RequiredArgsConstructor
public class ScmRebateService {

    /** 返利协议号的编码规则标识 */
    public static final String DOC_NO = "FL";


    private final ScmRebateMapper rebateMapper;
    private final ScmCustomerService customerService;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;

    /** 计算结果：销售额、返利率、返利金额 */
    public record Calc(BigDecimal salesAmount, BigDecimal rate, BigDecimal rebateAmount) {
    }

    public PageResult<ScmRebate> page(long current, long size, Long customerId, RebateStatus status, String keyword) {
        Page<ScmRebate> page = Page.of(current, size);
        rebateMapper.selectPageList(page, customerId, status, keyword);
        return PageResult.of(page);
    }

    public ScmRebate detail(Long id) {
        ScmRebate r = id == null ? null : rebateMapper.selectDetail(id);
        if (r == null) {
            throw new BizException("scm.error.rebateAgreementDoesNotExist");
        }
        r.setTiers(rebateMapper.selectTiers(id));
        return r;
    }

    @Transactional
    public Long create(RebateRequest req) {
        validate(req);
        ScmRebate r = new ScmRebate();
        r.setRebateNo(docNoService.next(DOC_NO));
        fill(r, req);
        r.setStatus(RebateStatus.ACTIVE);
        rebateMapper.insert(r);
        rebateMapper.insertTiers(r.getId(), tiers(req));
        return r.getId();
    }

    @Transactional
    public void update(Long id, RebateRequest req) {
        ScmRebate r = requireActive(id);
        validate(req);
        fill(r, req);
        rebateMapper.updateById(r);
        rebateMapper.deleteTiers(id);
        rebateMapper.insertTiers(id, tiers(req));
    }

    @Transactional
    public void delete(Long id) {
        requireActive(id);
        rebateMapper.deleteTiers(id);
        rebateMapper.deleteById(id);
    }

    /** 按当前数据计算（期间未结束时为截至今天的预估） */
    public Calc calc(Long id) {
        ScmRebate r = detail(id);
        LocalDate end = r.getEndDate().isAfter(LocalDate.now()) ? LocalDate.now() : r.getEndDate();
        return calc(r, end);
    }

    /** 结算：期间结束后按销售额计算返利；红字应收方式生成红字应收单 */
    @Transactional
    public Calc settle(Long id, LocalDate settleDate) {
        ScmRebate r = requireActive(id);
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
            eventPublisher.publishEvent(new RebateSettledEvent(id, r.getRebateNo(), r.getCustomerId(), c.rebateAmount(), date));
        }
        return c;
    }

    /** 撤销结算：红字应收单须未核销、未生成凭证；下次订单抵扣的须还没有被使用（余额足够） */
    @Transactional
    public void unsettle(Long id) {
        ScmRebate r = detail(id);
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
     * 记录返利结算（红字应收方式）生成的红字应收单。由财务模块在处理 {@link RebateSettledEvent} 时调用，与结算在同一事务
     */
    public void bindBill(Long rebateId, Long billId) {
        rebateMapper.update(Wrappers.<ScmRebate>lambdaUpdate().set(ScmRebate::getBillId, billId).eq(ScmRebate::getId, rebateId));
    }

    /**
     * 客户可用的返利余额：已结算的「下次订单抵扣」返利 - 销售订单已抵扣
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
        BigDecimal sales = rebateMapper.selectSalesAmount(r.getCustomerId(), r.getBeginDate(), end);
        BigDecimal rate = r.getTiers().stream().filter(t -> sales.compareTo(t.getMinAmount()) >= 0)
                .max(Comparator.comparing(ScmRebateTier::getMinAmount)).map(ScmRebateTier::getRate).orElse(BigDecimal.ZERO);
        return new Calc(sales, rate, sales.multiply(rate).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP));
    }

    private ScmRebate requireActive(Long id) {
        ScmRebate r = detail(id);
        if (r.getStatus() != RebateStatus.ACTIVE) {
            throw new BizException("scm.error.rebateAgreementHasSettledCannotModified");
        }
        return r;
    }

    private void validate(RebateRequest req) {
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("scm.error.endDateCannotEarlierStartDate");
        }
        if (req.tiers().stream().map(t -> t.minAmount().stripTrailingZeros()).collect(HashSet::new, HashSet::add, HashSet::addAll).size()
                != req.tiers().size()) {
            throw new BizException("scm.error.rebateTierSalesAmountsCannotDuplicated");
        }
        ScmCustomer customer = customerService.get(req.customerId());
        if (customer.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("scm.error.customerDisabled", customer.getName());
        }
    }

    private static void fill(ScmRebate r, RebateRequest req) {
        r.setCustomerId(req.customerId());
        r.setBeginDate(req.beginDate());
        r.setEndDate(req.endDate());
        r.setSettleMode(req.settleMode());
        r.setRemark(req.remark());
    }

    private static List<ScmRebateTier> tiers(RebateRequest req) {
        return req.tiers().stream().map(t -> {
            ScmRebateTier tier = new ScmRebateTier();
            tier.setMinAmount(t.minAmount());
            tier.setRate(t.rate());
            return tier;
        }).toList();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
