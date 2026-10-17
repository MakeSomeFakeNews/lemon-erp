package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单的门户通知：采购订单审核通过后通知供应商在门户确认
 */
@Component
@RequiredArgsConstructor
public class ScmOrderNotifier {

    private final ScmPortalNotifier portalNotifier;

    /** 订单审核通过（直接审核或审批通过）：采购订单通知供应商，销售订单不通知 */
    public void approved(ScmOrder order) {
        if (order.getKind() == OrderKind.PURCHASE) {
            portalNotifier.notify(order.getPartnerId(), MessageCategory.REMIND, "新采购订单待确认：" + order.getOrderNo(), // i18n-ignore
                    "订单金额 " + order.getTotalAmount().stripTrailingZeros().toPlainString() + "，请确认并填写承诺交期", "/portal/order"); // i18n-ignore
        }
    }
}
