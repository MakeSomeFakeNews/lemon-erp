package com.lemon.erp.modules.scm.event;

/**
 * 订单反审核（回到草稿）、关闭（与操作同一事务）。下游模块（生产计划等）监听：反审核时有下游单据可以抛异常阻止，关闭时联动关闭
 *
 * @param kind   PURCHASE / SALES
 * @param status 变更后的状态：DRAFT 反审核 / CLOSED 关闭
 */
public record OrderStatusChangedEvent(String kind, Long orderId, String orderNo, String status) {
}
