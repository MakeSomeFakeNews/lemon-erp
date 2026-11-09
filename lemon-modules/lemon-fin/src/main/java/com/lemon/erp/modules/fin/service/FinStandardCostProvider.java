package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.StandardCostProvider;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import com.lemon.erp.modules.fin.entity.FinStdCostVersion;
import com.lemon.erp.modules.fin.mapper.FinStdCostMapper;
import com.lemon.erp.modules.fin.mapper.FinStdCostVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 生产完工入库的实时成本取所在期间生效的标准成本版本（含料工费构成） */
@Component
@RequiredArgsConstructor
public class FinStandardCostProvider implements StandardCostProvider {

    private final FinStdCostVersionMapper versionMapper;
    private final FinStdCostMapper costMapper;

    @Override
    public Map<Long, StandardCost> costs(LocalDate bizDate, Collection<Long> materialIds) {
        Map<Long, StandardCost> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        FinStdCostVersion version = versionMapper.selectEffective(bizDate.getYear() * 100 + bizDate.getMonthValue());
        if (version == null) {
            return map;
        }
        costMapper.selectList(Wrappers.<FinStdCost>lambdaQuery()
                        .select(FinStdCost::getMaterialId, FinStdCost::getUnitCost, FinStdCost::getMaterialCost,
                                FinStdCost::getLaborCost, FinStdCost::getOverheadCost)
                        .eq(FinStdCost::getVersionId, version.getId())
                        .in(FinStdCost::getMaterialId, materialIds)
                        .gt(FinStdCost::getUnitCost, BigDecimal.ZERO))
                .forEach(c -> map.put(c.getMaterialId(), breakdown(c)));
        return map;
    }

    /** 料工费为空（如采购件、早期版本）时整个单价计为材料；三者之和与单价不一致时以单价为准，差额计入材料 */
    private static StandardCost breakdown(FinStdCost c) {
        BigDecimal labor = Decimals.nz(c.getLaborCost());
        BigDecimal overhead = Decimals.nz(c.getOverheadCost());
        return new StandardCost(c.getUnitCost(), c.getUnitCost().subtract(labor).subtract(overhead).max(BigDecimal.ZERO), labor, overhead);
    }
}
