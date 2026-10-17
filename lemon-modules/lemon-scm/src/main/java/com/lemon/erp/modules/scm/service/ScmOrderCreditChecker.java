package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.finance.PartnerBalanceProvider;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.modules.scm.dto.CreditUsage;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.CreditControl;
import com.lemon.erp.modules.scm.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 销售订单的客户信用额度：审核时、出库前校验，客户详情显示占用情况。按业务参数 scm.credit.control 决定何时控制，规则见 {@link CreditRules}
 */
@Component
@RequiredArgsConstructor
public class ScmOrderCreditChecker {

    /** 占用信用额度的订单状态 */
    private static final Set<OrderStatus> OPEN_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(OrderStatus.APPROVING, OrderStatus.APPROVED, OrderStatus.PARTIAL));

    private final ScmOrderMapper orderMapper;
    private final ScmCustomerService customerService;
    private final PartnerBalanceProvider balanceProvider;
    private final BizRules bizRules;

    /** 审核时：应收余额 + 其他未完成订单的未发货金额 + 本订单金额不能超过额度 */
    public void checkOnApprove(ScmOrder order) {
        ScmCustomer customer = customerService.get(order.getPartnerId());
        if (customer.getCreditLimit() == null || !CreditRules.checksOnApprove(control())) {
            return;
        }
        BigDecimal open = orderMapper.selectOpenSalesAmount(customer.getId(), order.getId(), OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customer.getId());
        BigDecimal used = CreditRules.usageOnApprove(open, receivable, order.getTotalAmount(), order.getExchangeRate());
        if (CreditRules.exceeds(used, customer.getCreditLimit())) {
            throw new BizException("scm.error.exceedsCreditLimitCustomerLimitReceivable",
                    customer.getName(), plain(customer.getCreditLimit()), plain(receivable), plain(open), plain(order.getTotalAmount()));
        }
    }

    /**
     * 出库前再校验（审核后客户可能新增了欠款）：应收余额 + 全部未完成订单的未发货金额不能超过额度
     */
    public void checkBeforeShipment(ScmOrder order) {
        ScmCustomer customer = customerService.get(order.getPartnerId());
        if (customer.getCreditLimit() == null || !CreditRules.checksOnShipment(control())) {
            return;
        }
        BigDecimal open = orderMapper.selectOpenSalesAmount(customer.getId(), 0L, OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customer.getId());
        if (CreditRules.exceeds(CreditRules.usageOnShipment(open, receivable), customer.getCreditLimit())) {
            throw new BizException("scm.error.customerHasExceededCreditLimitDeliveries",
                    customer.getName(), plain(customer.getCreditLimit()), plain(receivable), plain(open));
        }
    }

    /** 信用占用：应收余额、未完成订单金额（客户详情显示） */
    public CreditUsage usage(Long customerId) {
        ScmCustomer customer = customerService.get(customerId);
        BigDecimal open = orderMapper.selectOpenSalesAmount(customerId, 0L, OPEN_STATUSES);
        BigDecimal receivable = balanceProvider.receivableBalance(customerId);
        return new CreditUsage(customer.getCreditLimit(), receivable, open);
    }

    private CreditControl control() {
        return bizRules.option(ScmBizRules.CREDIT_CONTROL, CreditControl.class);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
