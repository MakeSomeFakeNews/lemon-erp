package com.lemon.erp.modules.scm.event;

import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;

/**
 * 订单反审核（回到草稿）、关闭（与操作同一事务）。下游模块（生产计划等）监听：反审核时有下游单据可以抛异常阻止，关闭时联动关闭
 *
 * @param kind   订单种类
 * @param status 变更后的状态：DRAFT 反审核 / CLOSED 关闭
 */
public record OrderStatusChangedEvent(OrderKind kind, Long orderId, String orderNo, OrderStatus status) {
}
