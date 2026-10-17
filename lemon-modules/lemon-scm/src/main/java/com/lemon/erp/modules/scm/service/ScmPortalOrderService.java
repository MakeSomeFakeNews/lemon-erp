package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.SupplierConfirm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商门户的采购订单：查看本供应商已审核的订单，接受（填写承诺交期）或拒绝，结果通知制单人
 */
@Service
@RequiredArgsConstructor
public class ScmPortalOrderService {

    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper itemMapper;
    private final MessageSender messageSender;

    public PageResult<ScmOrder> page(long current, long size, Long supplierId, Boolean pending, String keyword) {
        Page<ScmOrder> page = Page.of(current, size);
        orderMapper.selectPortalPage(page, supplierId, pending, keyword);
        return PageResult.of(page);
    }

    public ScmOrder detail(Long id, Long supplierId) {
        ScmOrder order = id == null ? null : orderMapper.selectById(id);
        if (order == null || order.getKind() != OrderKind.PURCHASE || !order.getPartnerId().equals(supplierId)
                || !OrderStatus.AUDITED.contains(order.getStatus())) {
            throw new BizException("scm.error.purchaseOrderDoesNotExist");
        }
        ScmOrder detail = orderMapper.selectDetail(id);
        detail.setItems(itemMapper.selectByOrderId(id));
        return detail;
    }

    /** 接受时须填写承诺交期，拒绝时须填写原因；已入库前可以重新确认 */
    @Transactional
    public void confirm(Long id, Long supplierId, SrmRequests.OrderConfirm req) {
        ScmOrder order = detail(id, supplierId);
        if (!order.getStatus().isExecutable()) {
            throw new BizException("scm.error.orderCompletedClosed");
        }
        boolean accept = req.accept();
        if (accept && req.promisedDate() == null) {
            throw new BizException("scm.error.enterPromisedDeliveryDate");
        }
        if (!accept && (req.remark() == null || req.remark().isBlank())) {
            throw new BizException("scm.error.enterRejectionReason");
        }
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate()
                .set(ScmOrder::getSupplierConfirm, accept ? SupplierConfirm.ACCEPTED : SupplierConfirm.REJECTED)
                .set(ScmOrder::getPromisedDate, accept ? req.promisedDate() : null)
                .set(ScmOrder::getConfirmRemark, req.remark())
                .set(ScmOrder::getConfirmTime, LocalDateTime.now())
                .eq(ScmOrder::getId, id));
        String result = accept ? "已接受，承诺交期 " + req.promisedDate() : "已拒绝：" + req.remark(); // i18n-ignore
        messageSender.send(List.of(order.getCreateBy()), MessageSender.Message.of(accept ? MessageCategory.REMIND : MessageCategory.ALERT,
                "供应商" + (accept ? "接受" : "拒绝") + "了采购订单 " + order.getOrderNo(), order.getPartnerName() + " " + result, "/scm/purchase")); // i18n-ignore
    }
}
