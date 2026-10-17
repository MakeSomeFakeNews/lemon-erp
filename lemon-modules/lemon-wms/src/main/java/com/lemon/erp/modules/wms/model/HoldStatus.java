package com.lemon.erp.modules.wms.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 冻结状态：库存冻结（来料检验）的状态（字典 wms_hold_status）
 */
public enum HoldStatus {
    /** 待检 */
    PENDING,
    /** 不合格（待退货） */
    REJECTED,
    /** 已放行 */
    RELEASED,
    /** 已退货 */
    RETURNED;

    /** 仍冻结中（待检、不合格），可以放行或退货 */
    public static final Set<HoldStatus> HOLDING = EnumSet.of(PENDING, REJECTED);

    /** 仍冻结中 */
    public boolean isHolding() {
        return HOLDING.contains(this);
    }
}
