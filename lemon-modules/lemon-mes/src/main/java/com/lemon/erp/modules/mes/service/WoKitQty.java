package com.lemon.erp.modules.mes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按已领料计算工单可完工的齐套数量（纯函数），用于完工入库的领料校验：
 * <ul>
 *   <li>每个用料可支持的产品数量 = 净领料（领料 + 补料 − 退料，即工单用料的已领数量）/ 单位用量（含损耗），齐套数量取各用料的最小值，
 *       向下保留 4 位小数</li>
 *   <li>做过替代料切换的主料与替代料合并计算（切换后两者的单位用量按切换的数量拆分）：
 *       (主料已领 + Σ 替代料已领 / 替代比例) / (主料单位用量 + Σ 替代料单位用量 / 替代比例)，主料少领的部分可以由替代料补足</li>
 *   <li>单位用量为 0 的用料不参与；虚拟件在工单用料中已展开为子件，返工工单的用料是产品本身（单位用量 1），同样参与</li>
 *   <li>没有参与计算的用料时为「不限」（返回 qty 为 null）</li>
 * </ul>
 */
public final class WoKitQty {

    private WoKitQty() {
    }

    /**
     * 一个工单用料
     *
     * @param materialId 物料
     * @param unitQty    单位用量（含损耗）
     * @param issuedQty  净领料数量
     */
    public record Line(Long materialId, BigDecimal unitQty, BigDecimal issuedQty) {
    }

    /**
     * @param qty           齐套数量，为空表示不限
     * @param limitMaterial 限制齐套数量的物料（替代料组为主料），不限时为空
     */
    public record Result(BigDecimal qty, Long limitMaterial) {
        public static final Result UNLIMITED = new Result(null, null);
    }

    /**
     * @param lines       工单用料
     * @param substitutes 主料 → (替代料 → 替代比例：替代 1 个主料需要的数量)，只需包含工单用料中的主料
     */
    public static Result compute(List<Line> lines, Map<Long, Map<Long, BigDecimal>> substitutes) {
        Map<Long, Line> byMaterial = new LinkedHashMap<>();
        for (Line l : lines) {
            byMaterial.merge(l.materialId(), l, (a, b) -> new Line(a.materialId(), a.unitQty().add(b.unitQty()), a.issuedQty().add(b.issuedQty())));
        }
        Set<Long> grouped = new HashSet<>();
        BigDecimal min = null;
        Long limit = null;
        // 先算有替代料在用料中的主料（含替代料），再算其余用料
        for (Line main : byMaterial.values()) {
            Map<Long, BigDecimal> subs = substitutes.getOrDefault(main.materialId(), Map.of());
            BigDecimal unit = nz(main.unitQty());
            BigDecimal issued = nz(main.issuedQty());
            boolean hasSub = false;
            for (Map.Entry<Long, BigDecimal> s : subs.entrySet()) {
                Line sub = byMaterial.get(s.getKey());
                BigDecimal ratio = s.getValue();
                if (sub == null || grouped.contains(sub.materialId()) || sub.materialId().equals(main.materialId())
                        || ratio == null || ratio.signum() <= 0) {
                    continue;
                }
                hasSub = true;
                grouped.add(sub.materialId());
                unit = unit.add(nz(sub.unitQty()).divide(ratio, 10, RoundingMode.HALF_UP));
                issued = issued.add(nz(sub.issuedQty()).divide(ratio, 10, RoundingMode.HALF_UP));
            }
            if (!hasSub) {
                continue;
            }
            grouped.add(main.materialId());
            if (unit.signum() > 0) {
                BigDecimal q = cover(issued, unit);
                if (min == null || q.compareTo(min) < 0) {
                    min = q;
                    limit = main.materialId();
                }
            }
        }
        for (Line l : byMaterial.values()) {
            if (grouped.contains(l.materialId()) || nz(l.unitQty()).signum() <= 0) {
                continue;
            }
            BigDecimal q = cover(nz(l.issuedQty()), l.unitQty());
            if (min == null || q.compareTo(min) < 0) {
                min = q;
                limit = l.materialId();
            }
        }
        return min == null ? Result.UNLIMITED : new Result(min, limit);
    }

    private static BigDecimal cover(BigDecimal issued, BigDecimal unit) {
        return issued.max(BigDecimal.ZERO).divide(unit, 4, RoundingMode.DOWN);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
