package com.lemon.erp.modules.scm.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态，与字典 scm_order_status 对应。
 * 草稿 → 审批中（发布了审批流程时）→ 已审核 → 部分入库 / 出库 → 已完成；已审核、部分完成的订单可以手工关闭
 */
public enum OrderStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 已审核 */
    APPROVED,
    /** 部分入库 / 出库 */
    PARTIAL,
    /** 已完成 */
    COMPLETED,
    /** 已关闭 */
    CLOSED;

    /** 可以入库 / 出库的状态 */
    public static final Set<OrderStatus> EXECUTABLE = Collections.unmodifiableSet(EnumSet.of(APPROVED, PARTIAL));

    /** 可以退货的状态：只要有过入库 / 出库 */
    public static final Set<OrderStatus> RETURNABLE = Collections.unmodifiableSet(EnumSet.of(PARTIAL, COMPLETED, CLOSED));

    /** 未结束的订单：MRP 计算在途数量、销售信用额度占用 */
    public static final Set<OrderStatus> OPEN = Collections.unmodifiableSet(EnumSet.of(DRAFT, APPROVING, APPROVED, PARTIAL));

    /** 已审核（含审核后的执行、完成、关闭），供应商门户可见 */
    public static final Set<OrderStatus> AUDITED = Collections.unmodifiableSet(EnumSet.of(APPROVED, PARTIAL, COMPLETED, CLOSED));

    /** 可以入库 / 出库 */
    public boolean isExecutable() {
        return EXECUTABLE.contains(this);
    }

    /** 未审核（草稿、审批中） */
    public boolean isUnaudited() {
        return this == DRAFT || this == APPROVING;
    }
}
