package com.lemon.erp.modules.wms.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单据列表的「仓库」列：明细可另选仓库（为空表示表头仓库）时，汇总每张单据实际涉及的仓库名称。
 * 只有一个仓库时列表显示该仓库，多个时显示「多仓」并可悬停查看
 */
public final class LineWarehouses {

    private LineWarehouses() {
    }

    /**
     * @param rows        各单据明细的仓库：[{docId, warehouseId（为空表示表头仓库）, warehouseName}]，每张单据每个仓库一行
     * @param headerNames 单据 ID → 表头仓库名称
     * @return 单据 ID → 实际涉及的仓库名称（表头仓库在前，去重）
     */
    public static Map<Long, List<String>> names(List<Map<String, Object>> rows, Map<Long, String> headerNames) {
        Map<Long, Set<String>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long docId = ((Number) row.get("docId")).longValue();
            String name = row.get("warehouseId") == null ? headerNames.get(docId) : (String) row.get("warehouseName");
            if (name != null) {
                map.computeIfAbsent(docId, k -> new LinkedHashSet<>()).add(name);
            }
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        map.forEach((docId, names) -> {
            List<String> list = new ArrayList<>(names);
            String header = headerNames.get(docId);
            if (header != null && list.remove(header)) {
                list.add(0, header);
            }
            result.put(docId, list);
        });
        return result;
    }
}
