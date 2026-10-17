package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.mes.entity.MesBomItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单用料的 BOM 计算（纯计算，不依赖 Spring，BOM 数据由 {@link BomSource} 提供）：
 * <ul>
 *   <li>含损耗的单位用量 = 用量 × (1 + 损耗率 / 100)，6 位小数</li>
 *   <li>虚拟件展开：子件为虚拟件（且有默认 BOM）时用它的子件代替，用量逐层相乘，同一物料合并（保持首次出现的顺序）；
 *       超过 {@link #MAX_PHANTOM_DEPTH} 层视为循环引用</li>
 *   <li>需求数量 = 工单数量 × 单位用量，序列号物料向上取整，其他 4 位小数</li>
 * </ul>
 */
public final class WoBomExpander {

    /** 虚拟件最大展开层数，超过视为循环引用 */
    public static final int MAX_PHANTOM_DEPTH = 20;

    /** 展开所需的 BOM 数据 */
    public interface BomSource {

        /**
         * 给定物料中设为虚拟件且有默认 BOM 的：物料 ID → 默认 BOM ID（每层调用一次）
         */
        Map<Long, Long> phantomBoms(List<Long> materialIds);

        /** BOM 的子件 */
        List<MesBomItem> items(Long bomId);
    }

    private WoBomExpander() {
    }

    /**
     * 展开 BOM 子件中的虚拟件
     *
     * @return 物料 ID → 单位用量（未取整，保持首次出现的顺序）
     */
    public static Map<Long, BigDecimal> expand(List<MesBomItem> items, BomSource source) {
        Map<Long, BigDecimal> unitQtys = new LinkedHashMap<>();
        expand(items, BigDecimal.ONE, unitQtys, 0, source);
        return unitQtys;
    }

    private static void expand(List<MesBomItem> items, BigDecimal factor, Map<Long, BigDecimal> unitQtys, int depth, BomSource source) {
        if (depth > MAX_PHANTOM_DEPTH) {
            throw new BizException("mes.error.phantomItemsExceedLevelsMayCircular", MAX_PHANTOM_DEPTH);
        }
        Map<Long, Long> phantomBoms = source.phantomBoms(items.stream().map(MesBomItem::getMaterialId).toList());
        for (MesBomItem item : items) {
            BigDecimal unit = factor.multiply(unitQty(item));
            Long sub = phantomBoms.get(item.getMaterialId());
            if (sub != null) {
                expand(source.items(sub), unit, unitQtys, depth + 1, source);
            } else {
                unitQtys.merge(item.getMaterialId(), unit, BigDecimal::add);
            }
        }
    }

    /** 含损耗的单位用量 = 用量 × (1 + 损耗率 / 100) */
    public static BigDecimal unitQty(MesBomItem item) {
        return item.getQty().multiply(BigDecimal.ONE.add(item.getLossRate().divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP)))
                .setScale(6, RoundingMode.HALF_UP);
    }

    /** 工单用料的单位用量：6 位小数 */
    public static BigDecimal roundUnitQty(BigDecimal unitQty) {
        return unitQty.setScale(6, RoundingMode.HALF_UP);
    }

    /** 需求数量 = 工单数量 × 单位用量；序列号物料向上取整，其他 4 位小数 */
    public static BigDecimal requiredQty(BigDecimal woQty, BigDecimal unitQty, boolean serial) {
        BigDecimal required = woQty.multiply(unitQty);
        return serial ? required.setScale(0, RoundingMode.CEILING) : required.setScale(4, RoundingMode.HALF_UP);
    }
}
