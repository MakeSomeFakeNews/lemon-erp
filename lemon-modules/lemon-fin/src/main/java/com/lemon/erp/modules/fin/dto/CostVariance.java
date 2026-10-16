package com.lemon.erp.modules.fin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工单成本差异：实际成本（各期工单成本合计）与完工数量 × 单位标准成本比较。
 * <ul>
 *   <li>材料价格差异 = 实际材料成本 - Σ 实际耗用量 × 子件标准成本</li>
 *   <li>材料用量差异 = Σ 实际耗用量 × 子件标准成本 - 完工数量 × 单位标准材料成本（含实际所用 BOM 与标准 BOM 的不同）</li>
 *   <li>人工效率差异 = (实际工时 - 标准工时) × 标准小时费率，人工费率差异 = 实际人工 - 实际工时 × 标准小时费率</li>
 *   <li>制造费用的效率差异、耗费差异同理；六项之和 = 实际成本 - 标准成本</li>
 * </ul>
 */
@Data
public class CostVariance {

    private Long woId;
    private String woNo;
    private String status;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal finishedQty;
    private BigDecimal hours;
    private BigDecimal actualMaterial;
    private BigDecimal actualLabor;
    private BigDecimal actualOverhead;
    /** 关闭工单转出的生产损失（已包含在实际成本中） */
    private BigDecimal lossAmount;
    /** 产品没有标准成本时为 false */
    private Boolean hasStd;
    private BigDecimal stdUnitMaterial;
    private BigDecimal stdUnitLabor;
    private BigDecimal stdUnitOverhead;
    private BigDecimal stdUnitHours;
    /** Σ 实际耗用量 × 子件标准成本 */
    private BigDecimal issuedAtStd;
    /** 没有标准成本的子件数 */
    private Integer missingStd;

    private BigDecimal actualTotal;
    private BigDecimal stdMaterial;
    private BigDecimal stdLabor;
    private BigDecimal stdOverhead;
    private BigDecimal stdTotal;
    private BigDecimal stdHours;
    private BigDecimal materialPrice;
    private BigDecimal materialUsage;
    private BigDecimal laborRate;
    private BigDecimal laborEfficiency;
    private BigDecimal overheadSpending;
    private BigDecimal overheadEfficiency;
    private BigDecimal totalVariance;
}
