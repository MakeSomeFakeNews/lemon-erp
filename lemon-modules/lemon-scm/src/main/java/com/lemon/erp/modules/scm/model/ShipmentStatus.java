package com.lemon.erp.modules.scm.model;

/**
 * 发运状态，与字典 scm_shipment_status 对应：待发运 → 运输中 → 已签收，运输中可标记异常，异常处理后可继续运输或签收
 */
public enum ShipmentStatus {
    PENDING,
    SHIPPED,
    SIGNED,
    EXCEPTION
}
