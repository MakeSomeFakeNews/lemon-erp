package com.lemon.erp.modules.scm.model;

import java.util.List;

/**
 * 订单状态，与字典 scm_order_status 对应。
 * 草稿 → 审批中（发布了审批流程时）→ 已审核 → 部分入库 / 出库 → 已完成；已审核、部分完成的订单可以手工关闭
 */
public enum OrderStatus {
    DRAFT,
    APPROVING,
    APPROVED,
    PARTIAL,
    COMPLETED,
    CLOSED;

    /** 可以入库 / 出库的状态 */
    public static final List<String> EXECUTABLE = List.of(APPROVED.name(), PARTIAL.name());

    /** 可以退货的状态：只要有过入库 / 出库 */
    public static final List<String> RETURNABLE = List.of(PARTIAL.name(), COMPLETED.name(), CLOSED.name());

    /** 未结束的订单：MRP 计算在途数量、销售信用额度占用 */
    public static final List<String> OPEN = List.of(DRAFT.name(), APPROVING.name(), APPROVED.name(), PARTIAL.name());
}
