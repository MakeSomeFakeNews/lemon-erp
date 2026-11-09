package com.lemon.erp.modules.mes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 完工入库按工单实际领料成本计价（纯函数）。工单的净领料成本 = 领料、补料出库的成本 − 退料入库的成本，按完工数量分摊：
 * <ul>
 *   <li>最后一次完工（累计完工达到工单数量）取「净领料成本 − 之前完工入库已转入的成本」，累计入库成本精确等于净领料成本，不留尾差，
 *       超领的物料全部由最后一次完工承担</li>
 *   <li>其余各次按 BOM 单位用量：每种物料取 min(本次应耗 = 需求 × 本次数量 / 工单数量, 尚未被之前完工耗用的已领数量) × 该物料的净领料单价；
 *       未领或少领的物料只计已领部分，合计不超过尚未转入的成本；领用的物料都没有 BOM 用量时按剩余数量平均分摊</li>
 *   <li>工单没有已计价的领料（未领料，或全部退回）时返回 null，由调用方按其他口径（当前均价）计价</li>
 * </ul>
 */
public final class WoFinishCost {

    private WoFinishCost() {
    }

    /**
     * 一种物料的净领料
     *
     * @param qty      净领料数量（领料 + 补料 − 退料）
     * @param amount   净领料成本
     * @param required 工单需求数量，不在用料中（没有 BOM 用量）为 0
     */
    public record Issued(BigDecimal qty, BigDecimal amount, BigDecimal required) {
    }

    /**
     * @param woQty          工单数量
     * @param finishedBefore 本次之前的累计完工数量
     * @param qty            本次完工数量
     * @param issued         物料 ID → 净领料
     * @param absorbed       之前完工入库已转入的成本合计
     * @return 本次完工入库的成本金额（2 位小数），没有已计价的领料时为 null
     */
    public static BigDecimal amount(BigDecimal woQty, BigDecimal finishedBefore, BigDecimal qty, Map<Long, Issued> issued,
                                    BigDecimal absorbed) {
        BigDecimal net = issued.values().stream().map(Issued::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (net.signum() <= 0 || qty.signum() <= 0) {
            return null;
        }
        BigDecimal remaining = net.subtract(absorbed).max(BigDecimal.ZERO);
        if (finishedBefore.add(qty).compareTo(woQty) >= 0 || woQty.signum() <= 0) {
            return money(remaining);
        }
        if (issued.values().stream().noneMatch(m -> m.required().signum() > 0)) {
            // 领用的物料都没有 BOM 用量：按剩余数量平均分摊
            return money(remaining.multiply(qty).divide(woQty.subtract(finishedBefore), 10, RoundingMode.HALF_UP));
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Issued m : issued.values()) {
            if (m.qty().signum() <= 0 || m.amount().signum() <= 0 || m.required().signum() <= 0) {
                continue;
            }
            BigDecimal need = m.required().multiply(qty).divide(woQty, 10, RoundingMode.HALF_UP);
            BigDecimal used = m.required().multiply(finishedBefore).divide(woQty, 10, RoundingMode.HALF_UP);
            BigDecimal take = need.min(m.qty().subtract(used).max(BigDecimal.ZERO));
            sum = sum.add(take.multiply(m.amount()).divide(m.qty(), 10, RoundingMode.HALF_UP));
        }
        return money(sum.min(remaining));
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
