package com.lemon.erp.modules.scm.model;

import java.util.List;

/**
 * 草稿订单被删除（同一事务中同步发布）：下游模块解除对订单的引用，如商机的「已生成销售订单」
 *
 * @param kind PURCHASE / SALES
 */
public record OrderDeletedEvent(String kind, List<Long> orderIds) {
}
