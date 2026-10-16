package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.quality.SupplierQualityProvider;
import com.lemon.erp.modules.mes.mapper.MesInspectionMapper;
import com.lemon.erp.modules.mes.mapper.QmsSupplierClaimMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 按已确认的来料检验统计供应商的批次合格率，按已确认的质量索赔统计索赔次数和金额
 */
@Component
@RequiredArgsConstructor
public class MesSupplierQualityProvider implements SupplierQualityProvider {

    private final MesInspectionMapper inspectionMapper;
    private final QmsSupplierClaimMapper claimMapper;

    @Override
    public Map<Long, QualityStat> qualityStats(Collection<Long> supplierIds, LocalDate begin, LocalDate end) {
        Map<Long, QualityStat> map = new HashMap<>();
        if (supplierIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : inspectionMapper.selectIqcStats(supplierIds, begin, end)) {
            map.put(((Number) row.get("supplierId")).longValue(), new QualityStat(((Number) row.get("batches")).longValue(),
                    ((Number) row.get("passBatches")).longValue(), 0, BigDecimal.ZERO));
        }
        for (Map<String, Object> row : claimMapper.selectStats(supplierIds, begin, end)) {
            Long supplierId = ((Number) row.get("supplierId")).longValue();
            QualityStat iqc = map.getOrDefault(supplierId, new QualityStat(0, 0, 0, BigDecimal.ZERO));
            map.put(supplierId, new QualityStat(iqc.iqcBatches(), iqc.iqcPassBatches(), ((Number) row.get("claims")).longValue(),
                    new BigDecimal(row.get("amount").toString())));
        }
        return map;
    }
}
