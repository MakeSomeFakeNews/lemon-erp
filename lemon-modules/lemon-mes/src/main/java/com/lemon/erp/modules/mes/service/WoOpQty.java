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

    /**
     * 按工序号填写各工序还可报工的数量（{@link MesWoOp#getReportableQty()}）：
     * 上道工序（工序号小于本工序的最后一道）的累计合格数，第一道工序为工单数量，减本工序已报的合格和报废，不小于 0。
     * 与报工校验（{@code WoReportService.report}）一致
     */
    static void fillReportable(BigDecimal woQty, List<MesWoOp> ops) {
        List<MesWoOp> sorted = ops.stream()
                .sorted(Comparator.comparing(MesWoOp::getSeq, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        for (MesWoOp op : sorted) {
            MesWoOp prev = null;
            for (MesWoOp o : sorted) {
                // 工序号相同的取第一道（与报工校验的 Stream.max 一致）
                if (o.getSeq() != null && op.getSeq() != null && o.getSeq() < op.getSeq()
                        && (prev == null || o.getSeq() > prev.getSeq())) {
                    prev = o;
                }
            }
            BigDecimal cap = prev == null ? woQty : nz(prev.getGoodQty()).min(woQty);
            op.setReportableQty(cap.subtract(nz(op.getGoodQty())).subtract(nz(op.getScrapQty())).max(BigDecimal.ZERO));
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
