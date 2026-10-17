package com.lemon.erp.modules.wms.service;

import com.lemon.erp.modules.wms.mapper.WmsPutawayMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 上架策略：入库时为每行推荐库位。顺序：物料的默认库位 → 已有同批次库存的库位 → 已有该物料库存的库位 → 没有库存的空存储位
 * （同一次推荐中一个空库位只推荐给一种物料）。只推荐启用的存储位、拣货位（默认库位除外）
 */
@Service
@RequiredArgsConstructor
public class WmsPutawayService {

    private final WmsPutawayMapper putawayMapper;

    public record Line(Long materialId, String batchNo) {
    }

    /** 推荐库位的依据 */
    public enum Reason {
        /** 物料的默认库位 */
        DEFAULT,
        /** 已存放同批次的库位 */
        SAME_BATCH,
        /** 已存放同物料的库位 */
        SAME_MATERIAL,
        /** 空库位 */
        EMPTY
    }

    /** @param reason 推荐依据，没有可推荐的库位时全部为空 */
    public record Suggestion(Long locationId, String locationCode, Reason reason) {
    }

    public List<Suggestion> suggest(Long warehouseId, List<Line> lines) {
        Set<Long> materialIds = new HashSet<>();
        lines.forEach(l -> materialIds.add(l.materialId()));
        Map<Long, Suggestion> defaults = new HashMap<>();
        Map<Long, List<Map<String, Object>>> stocked = new HashMap<>();
        if (!materialIds.isEmpty()) {
            putawayMapper.selectDefaultLocations(warehouseId, materialIds).forEach(r -> defaults.put(id(r, "materialId"),
                    new Suggestion(id(r, "locationId"), (String) r.get("locationCode"), Reason.DEFAULT)));
            putawayMapper.selectStockedLocations(warehouseId, materialIds).forEach(r ->
                    stocked.computeIfAbsent(id(r, "materialId"), k -> new ArrayList<>()).add(r));
        }
        List<Map<String, Object>> empty = null;
        Map<Long, Suggestion> emptyByMaterial = new HashMap<>();
        List<Suggestion> result = new ArrayList<>();
        for (Line line : lines) {
            Suggestion s = defaults.get(line.materialId());
            List<Map<String, Object>> rows = stocked.getOrDefault(line.materialId(), List.of());
            String batch = line.batchNo() == null ? "" : line.batchNo().trim();
            if (s == null && !batch.isEmpty()) {
                s = rows.stream().filter(r -> batch.equals(r.get("batchNo"))).findFirst()
                        .map(r -> new Suggestion(id(r, "locationId"), (String) r.get("locationCode"), Reason.SAME_BATCH)).orElse(null);
            }
            if (s == null && !rows.isEmpty()) {
                Map<String, Object> r = rows.get(0);
                s = new Suggestion(id(r, "locationId"), (String) r.get("locationCode"), Reason.SAME_MATERIAL);
            }
            if (s == null) {
                s = emptyByMaterial.get(line.materialId());
                if (s == null) {
                    if (empty == null) {
                        empty = new ArrayList<>(putawayMapper.selectEmptyLocations(warehouseId, lines.size()));
                    }
                    Iterator<Map<String, Object>> it = empty.iterator();
                    if (it.hasNext()) {
                        Map<String, Object> r = it.next();
                        it.remove();
                        s = new Suggestion(id(r, "locationId"), (String) r.get("locationCode"), Reason.EMPTY);
                        emptyByMaterial.put(line.materialId(), s);
                    }
                }
            }
            result.add(Objects.requireNonNullElse(s, new Suggestion(null, null, null)));
        }
        return result;
    }

    private static Long id(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : ((Number) v).longValue();
    }
}
