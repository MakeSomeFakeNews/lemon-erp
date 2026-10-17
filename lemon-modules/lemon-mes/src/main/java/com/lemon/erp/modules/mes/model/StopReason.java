package com.lemon.erp.modules.mes.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备停机原因的内置取值。停机原因是业务字典 mes_stop_reason（租户可新增），停机记录中以字符串保存；
 * 这里只用于判断内置原因是否计划停机：计划停机从 OEE 的计划生产时间中扣除，其余（含租户新增的原因）为可用率损失
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

    /** 是否计划停机：内置的 MAINTAIN、NO_ORDER 是，其他（含租户新增的原因）不是 */
    public static boolean isPlanned(String reason) {
        for (StopReason r : values()) {
            if (r.name().equals(reason)) {
                return r.planned;
            }
        }
        return false;
    }
}
