package com.lemon.erp.modules.mes.service;

import com.lemon.erp.modules.mes.entity.MesWoOp;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工序的剩余数量：前序工序报废的产品不会流到后面的工序，工序还要做的数量 = 工单数量 − 前序工序的报废 − 本工序的合格和报废
 * （生产看板、产能排程使用）
 */
final class WoOpQty {

    private WoOpQty() {
    }

    /** 按工单分组、工序号排序后填写各工序的前序报废合计（{@link MesWoOp#getUpstreamScrap()}） */
    static void fillUpstreamScrap(Collection<MesWoOp> ops) {
        Map<Long, List<MesWoOp>> byWo = ops.stream().collect(Collectors.groupingBy(MesWoOp::getWoId));
        for (List<MesWoOp> list : byWo.values()) {
            list.sort(Comparator.comparing(MesWoOp::getSeq, Comparator.nullsLast(Comparator.naturalOrder())));
            BigDecimal before = BigDecimal.ZERO;
            for (MesWoOp op : list) {
                op.setUpstreamScrap(before);
                before = before.add(nz(op.getScrapQty()));
            }
        }
    }

    /** 工序未完成的数量，可能为负数（调用方按 ≤ 0 视为完成） */
    static BigDecimal left(BigDecimal woQty, MesWoOp op) {
        return woQty.subtract(nz(op.getUpstreamScrap())).subtract(nz(op.getGoodQty())).subtract(nz(op.getScrapQty()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
