package com.lemon.erp.modules.mes.service;

import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.scm.model.AtpLine;
import com.lemon.erp.modules.scm.model.AtpProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可承诺量的生产供需：在制工单产出、工单待领料
 */
@Component
@RequiredArgsConstructor
public class MesAtpProvider implements AtpProvider {

    private final MesWorkOrderMapper workOrderMapper;

    @Override
    public List<AtpLine> lines(Long materialId, Long warehouseId) {
        return workOrderMapper.selectAtpLines(materialId, warehouseId).stream()
                .map(r -> new AtpLine(r.get("date") == null ? null : LocalDate.parse(r.get("date").toString().substring(0, 10)),
                        (String) r.get("type"), (String) r.get("docNo"), new BigDecimal(r.get("qty").toString())))
                .toList();
    }

    @Override
    public Map<Long, BigDecimal> netQty(Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (!materialIds.isEmpty()) {
            workOrderMapper.selectNetSupply(materialIds).forEach(r -> map.put(((Number) r.get("materialId")).longValue(),
                    new BigDecimal(r.get("qty").toString())));
        }
        return map;
    }

    @Override
    public Map<Long, String> supplyTypes(Collection<Long> materialIds) {
        Map<Long, String> map = new HashMap<>();
        if (!materialIds.isEmpty()) {
            workOrderMapper.selectSupplyTypes(materialIds).forEach(r -> map.put(((Number) r.get("materialId")).longValue(),
                    (String) r.get("supplyType")));
        }
        return map;
    }
}
