package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import com.lemon.erp.modules.scm.dto.RequisitionRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmPlanningMapper;
import com.lemon.erp.modules.scm.model.AtpProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 补货建议（再订货点法）：设置了再订货点的物料，预计库存不高于再订货点时建议补货，可一键生成请购单
 */
@Service
@RequiredArgsConstructor
public class ScmReorderService {

    private final ScmPlanningMapper planningMapper;
    private final ScmQueryService queryService;
    private final ScmRequisitionService requisitionService;
    private final List<AtpProvider> providers;

    /**
     * @param onlyNeeded 只返回需要补货的物料
     */
    public List<ReorderSuggestion> suggestions(String keyword, boolean onlyNeeded) {
        List<Map<String, Object>> rows = planningMapper.selectReorderMaterials(keyword);
        List<Long> ids = rows.stream().map(r -> ((Number) r.get("materialId")).longValue()).toList();
        Map<Long, BigDecimal> woNet = new HashMap<>();
        Map<Long, String> supplyTypes = new HashMap<>();
        for (AtpProvider p : providers) {
            p.netQty(ids).forEach((k, v) -> woNet.merge(k, v, BigDecimal::add));
            supplyTypes.putAll(p.supplyTypes(ids));
        }
        Map<Long, ScmSupplierPrice> defaults = queryService.defaultSuppliers(ids);
        return rows.stream().map(r -> {
            Long id = ((Number) r.get("materialId")).longValue();
            BigDecimal available = dec(r.get("available"));
            BigDecimal poOpen = dec(r.get("poOpen"));
            BigDecimal soOpen = dec(r.get("soOpen"));
            BigDecimal wo = woNet.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal projected = available.add(poOpen).add(wo).subtract(soOpen);
            BigDecimal reorderPoint = dec(r.get("reorderPoint"));
            BigDecimal reorderQty = r.get("reorderQty") == null ? null : dec(r.get("reorderQty"));
            BigDecimal safety = dec(r.get("safetyStock"));
            ScmSupplierPrice price = defaults.get(id);
            BigDecimal suggest = BigDecimal.ZERO;
            if (projected.compareTo(reorderPoint) <= 0) {
                suggest = reorderPoint.add(safety).subtract(projected);
                if (reorderQty != null && reorderQty.compareTo(suggest) > 0) {
                    suggest = reorderQty;
                }
                if (price != null && price.getMinQty() != null && price.getMinQty().compareTo(suggest) > 0) {
                    suggest = price.getMinQty();
                }
                suggest = suggest.setScale(0, RoundingMode.CEILING);
            }
            return new ReorderSuggestion(id, (String) r.get("code"), (String) r.get("name"), (String) r.get("spec"), (String) r.get("unit"),
                    supplyTypes.getOrDefault(id, AtpProvider.DEFAULT_SUPPLY_TYPE), r.get("safetyStock") == null ? null : safety, reorderPoint, reorderQty,
                    available, poOpen, soOpen, wo, projected, suggest,
                    price == null ? null : price.getSupplierId(), price == null ? null : price.getSupplierName(),
                    price == null ? null : queryService.effectivePrice(price, suggest));
        }).filter(s -> !onlyNeeded || s.suggestQty().signum() > 0).toList();
    }

    /**
     * 按选中的补货建议生成请购单（草稿）
     *
     * @return 请购单 ID
     */
    @Transactional
    public Long toRequisition(List<RequisitionRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException("scm.valid.selectMaterialsReplenish");
        }
        return requisitionService.create(new RequisitionRequest(LocalDate.now(), "补货建议生成", items)); // i18n-ignore
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
