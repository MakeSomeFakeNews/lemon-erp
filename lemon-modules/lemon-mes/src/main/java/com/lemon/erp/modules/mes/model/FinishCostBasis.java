package com.lemon.erp.modules.mes.model;

/**
 * 完工入库的实时成本口径（{@code mes_wo_doc.cost_basis}）
 */
public enum FinishCostBasis {
    /** 标准成本：产品在所在期间生效的标准成本版本中的单位标准成本（按料工费拆分） */
    STANDARD,
    /** 工单实际成本：净领料成本分摊 + 报工工时 × 工作中心人工 / 制造费用率 */
    ACTUAL,
    /** 当前均价：工单没有已计价的领料，也没有工费 */
    AVERAGE
}
