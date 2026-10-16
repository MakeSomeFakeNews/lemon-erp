package com.lemon.erp.modules.scm.model;

/**
 * 发货通知状态，与字典 scm_delivery_status 对应：待拣货 → 已拣货 → 已出库，出库前可以取消
 */
public enum DeliveryStatus {
    PENDING,
    PICKED,
    SHIPPED,
    CANCELLED
}
