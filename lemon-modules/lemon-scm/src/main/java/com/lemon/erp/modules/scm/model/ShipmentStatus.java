package com.lemon.erp.modules.scm.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 发运状态，与字典 scm_shipment_status 对应：待发运 → 运输中 → 已签收，运输中可标记异常，异常处理后可继续运输或签收
 */
public enum ShipmentStatus {
    /** 待发运 */
    PENDING,
    /** 运输中 */
    SHIPPED,
    /** 已签收 */
    SIGNED,
    /** 异常 */
    EXCEPTION;

    /** 在途（运输中或异常）：可以追加跟踪记录、签收 */
    public static final Set<ShipmentStatus> IN_TRANSIT = Collections.unmodifiableSet(EnumSet.of(SHIPPED, EXCEPTION));

    /** 在途（运输中或异常） */
    public boolean isInTransit() {
        return IN_TRANSIT.contains(this);
    }
}
