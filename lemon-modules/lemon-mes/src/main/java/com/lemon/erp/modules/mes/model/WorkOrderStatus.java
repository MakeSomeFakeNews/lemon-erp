package com.lemon.erp.modules.mes.model;

import java.util.List;

/**
 * 工单状态，与字典 mes_wo_status 对应：
 * 草稿 → 下达（生成用料、工序）→ 生产中（首次领料或报工）→ 已完工（完工入库满数量）；未完工的可以关闭
 */
public enum WorkOrderStatus {
    DRAFT,
    RELEASED,
    STARTED,
    COMPLETED,
    CLOSED;

    /** 可以领料、退料、报工、完工入库的状态 */
    public static final List<String> EXECUTABLE = List.of(RELEASED.name(), STARTED.name());

    /** 未结束的工单：MRP 计算在制数量与已分配用料 */
    public static final List<String> OPEN = List.of(DRAFT.name(), RELEASED.name(), STARTED.name());
}
