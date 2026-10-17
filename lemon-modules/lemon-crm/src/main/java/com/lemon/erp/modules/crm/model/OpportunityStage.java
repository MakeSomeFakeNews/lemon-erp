package com.lemon.erp.modules.crm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 销售阶段：商机销售阶段及默认赢单概率（字典 crm_opp_stage）
 */
@Getter
@AllArgsConstructor
public enum OpportunityStage {
    /** 初步接洽 */
    INITIAL(10),
    /** 需求确认 */
    NEEDS(30),
    /** 方案报价 */
    PROPOSAL(50),
    /** 商务谈判 */
    NEGOTIATION(80),
    /** 赢单 */
    WON(100),
    /** 输单 */
    LOST(0);

    /** 进行中的销售阶段，按推进顺序 */
    public static final List<OpportunityStage> OPEN_STAGES = List.of(INITIAL, NEEDS, PROPOSAL, NEGOTIATION);

    /** 默认赢单概率（%） */
    private final int probability;

    /** 赢单 / 输单为最终阶段 */
    public boolean isClosed() {
        return this == WON || this == LOST;
    }
}
