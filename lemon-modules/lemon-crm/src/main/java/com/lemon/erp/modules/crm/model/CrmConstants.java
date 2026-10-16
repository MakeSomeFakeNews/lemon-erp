package com.lemon.erp.modules.crm.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 客户关系管理的约定：销售阶段与默认赢单概率、工单优先级处理时限、产品保修期
 */
public final class CrmConstants {

    /** 进行中的销售阶段，按推进顺序 */
    public static final List<String> OPEN_STAGES = List.of("INITIAL", "NEEDS", "PROPOSAL", "NEGOTIATION");
    public static final Set<String> STAGES = Set.of("INITIAL", "NEEDS", "PROPOSAL", "NEGOTIATION", "WON", "LOST");
    public static final Map<String, Integer> STAGE_PROBABILITY = Map.of(
            "INITIAL", 10, "NEEDS", 30, "PROPOSAL", 50, "NEGOTIATION", 80, "WON", 100, "LOST", 0);
    /** 工单处理时限（小时） */
    public static final Map<String, Integer> SLA_HOURS = Map.of("URGENT", 4, "HIGH", 24, "MEDIUM", 72, "LOW", 168);
    /** 产品保修期（月），从销售出库日期起算 */
    public static final int WARRANTY_MONTHS = 12;

    private CrmConstants() {
    }
}
