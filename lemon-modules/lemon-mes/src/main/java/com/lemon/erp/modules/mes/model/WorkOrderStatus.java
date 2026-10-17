package com.lemon.erp.modules.mes.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 工单状态，与字典 mes_wo_status 对应：
 * 草稿 → 下达（生成用料、工序）→ 生产中（首次领料或报工）→ 已完工（完工入库满数量）；未完工的可以关闭
 */
public enum WorkOrderStatus {
    /** 草稿 */
    DRAFT,
    /** 已下达 */
    RELEASED,
    /** 生产中 */
    STARTED,
    /** 已完工 */
    COMPLETED,
    /** 已关闭 */
    CLOSED;

    /** 可以领料、退料、报工、完工入库的状态 */
    public static final Set<WorkOrderStatus> EXECUTABLE = Collections.unmodifiableSet(EnumSet.of(RELEASED, STARTED));

    /** 未结束的工单：MRP 计算在制数量与已分配用料 */
    public static final Set<WorkOrderStatus> OPEN = Collections.unmodifiableSet(EnumSet.of(DRAFT, RELEASED, STARTED));

    /** 是否可以领料、退料、报工、完工入库 */
    public boolean isExecutable() {
        return EXECUTABLE.contains(this);
    }

    /** 是否未结束（草稿、已下达、生产中） */
    public boolean isOpen() {
        return OPEN.contains(this);
    }
}
