package com.lemon.erp.modules.mes.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 质量统计：按检验类型，或按供应商（来料）汇总批次合格率与不良率
 */
@Data
public class QualitySummary {

    /** 检验类型 / 供应商编码 */
    private String groupKey;
    private String groupName;
    /** 按供应商汇总时的供应商 ID */
    private Long supplierId;

    private Long totalCount;
    private Long passCount;
    private Long failCount;
    private Long concessionCount;

    private BigDecimal sampleQty;
    private BigDecimal defectQty;

    /** 批次合格率（%）= 合格批次 / 总批次 */
    public BigDecimal getPassRate() {
        if (totalCount == null || totalCount == 0) {
            return null;
        }
        return BigDecimal.valueOf(passCount * 100L).divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }

    /** 抽样不良率（%）= 不良数 / 抽检数 */
    public BigDecimal getDefectRate() {
        if (sampleQty == null || sampleQty.signum() == 0) {
            return null;
        }
        return defectQty.multiply(BigDecimal.valueOf(100)).divide(sampleQty, 2, RoundingMode.HALF_UP);
    }
}
