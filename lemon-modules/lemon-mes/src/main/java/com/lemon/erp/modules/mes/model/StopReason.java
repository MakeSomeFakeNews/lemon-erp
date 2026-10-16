package com.lemon.erp.modules.mes.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备停机原因，与字典 mes_stop_reason 对应。计划停机从 OEE 的计划生产时间中扣除，其余为可用率损失
 */
@Getter
@AllArgsConstructor
public enum StopReason {
    BREAKDOWN(false),
    CHANGEOVER(false),
    MATERIAL(false),
    MAINTAIN(true),
    NO_ORDER(true),
    OTHER(false);

    /** 计划停机 */
    private final boolean planned;
}
