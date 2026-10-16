package com.lemon.erp.framework.quality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * 供应商质量：由质量管理按已确认的来料检验和供应商索赔统计，供应链的供应商绩效、评估使用
 */
public interface SupplierQualityProvider {

    /**
     * @param begin 检验日期、索赔日期起（含），为空不限
     * @param end   检验日期、索赔日期止（含），为空不限
     * @return 供应商 ID → 质量统计；没有检验和索赔的供应商不在结果中
     */
    Map<Long, QualityStat> qualityStats(Collection<Long> supplierIds, LocalDate begin, LocalDate end);

    /**
     * @param iqcBatches     来料检验批次
     * @param iqcPassBatches 合格批次（让步接收计为合格）
     * @param claims         已确认的质量索赔次数
     * @param claimAmount    已确认的质量索赔金额
     */
    record QualityStat(long iqcBatches, long iqcPassBatches, long claims, BigDecimal claimAmount) {
    }
}
