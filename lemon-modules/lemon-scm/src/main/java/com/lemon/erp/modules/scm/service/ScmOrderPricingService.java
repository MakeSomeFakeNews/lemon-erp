package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单价格：采购订单按采购合同价下单并校验合同占用，销售订单应用促销和返利抵扣，保存前按限价校验。
 * 在调用方（{@link ScmOrderService}）的事务中执行
 */
@Service
@RequiredArgsConstructor
public class ScmOrderPricingService {

    private final ScmContractUsageService contractUsage;
    private final ScmPromotionService promotionService;
    private final ScmRebateService rebateService;
    private final ScmPriceControlService priceControl;
    private final ScmCustomerService customerService;

    /** 采购订单引用采购合同：按合同价下单并校验 */
    public void applyContract(OrderKind kind, OrderRequest req, List<ScmOrderItem> items, Long orderId) {
        if (req.contractId() == null) {
            return;
        }
        if (kind != OrderKind.PURCHASE) {
            throw new BizException("scm.error.onlyPurchaseOrdersReferencePurchaseContract");
        }
        contractUsage.apply(req.contractId(), req.partnerId(), req.orderDate(), req.currency(), items, orderId);
    }

    /**
     * 审核时校验合同的剩余数量 / 金额、有效期
     *
     * @param items 订单明细，引用了合同时才读取
     */
    public void checkContractOnApprove(ScmOrder order, Supplier<List<ScmOrderItem>> items) {
        if (order.getContractId() != null) {
            contractUsage.checkOnApprove(order.getContractId(), order.getPartnerId(), order.getOrderDate(), items.get(), order.getId());
        }
    }

    /** 应用促销、返利抵扣，并按调整后的单价重算金额 */
    public ScmPromotionService.Applied promote(OrderRequest req, List<ScmOrderItem> items, Long orderId) {
        rebateService.checkBalance(req.partnerId(), req.rebateAmount(), orderId);
        ScmPromotionService.Applied applied = promotionService.apply(customerService.get(req.partnerId()), req.orderDate(), items,
                req.rebateAmount());
        for (ScmOrderItem item : applied.items()) {
            item.setAmount(amount(item.getQty(), item.getPrice()));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
        }
        return applied;
    }

    /** 订单记录的返利抵扣金额 */
    public static BigDecimal rebateOf(OrderRequest req) {
        return Decimals.money(req.rebateAmount());
    }

    /** 限价：控制方式为禁止保存的物料，销售单价不能低于最低销售价、采购单价不能高于最高采购价 */
    public void checkPriceLimit(OrderKind kind, List<ScmOrderItem> items, BigDecimal exchangeRate) {
        priceControl.check(kind, items, exchangeRate);
    }

    /** 订单保存后记录应用的促销（修改时先删除旧记录） */
    public void saveUsages(Long orderId, ScmPromotionService.Applied applied) {
        promotionService.saveUsages(orderId, applied.usages());
    }

    public void deleteUsages(Long orderId) {
        promotionService.deleteUsages(orderId);
    }
}
