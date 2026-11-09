package com.lemon.erp.modules.mes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * 完工入库的工费（直接人工、制造费用）按工单工序计算（纯函数），与月末成本核算同一口径：工时 × 工作中心的人工费率 / 制造费用率。
 * <ul>
 *   <li>工序的工费：有报工工时的按 累计报工工时 × 费率（报废品的工时由合格品承担）；没有报工工时（不使用报工，或报工没填工时）的
 *       按 标准工时 × 完工数量 × 费率（计划工时）</li>
 *   <li>非最后一次完工：有报工工时的工序取 min(本次数量, 该工序尚未被之前完工转出的合格数) × 工序工费 / 工序累计合格数，
 *       没有报工工时的工序取 本次数量 × 标准工时 × 费率；合计不超过尚未转入的工费</li>
 *   <li>最后一次完工（累计完工达到工单数量）：取全部已发生的工费 − 之前完工已转入的工费，不留尾差</li>
 * </ul>
 * 没有工艺路线的工单没有工费。
 */
public final class WoConversionCost {

    private WoConversionCost() {
    }

    /**
     * 一道工序
     *
     * @param hours        累计报工工时
     * @param stdHours     标准工时（小时 / 个）
     * @param goodQty      累计合格数量
     * @param laborRate    工作中心人工费率（元 / 工时）
     * @param overheadRate 工作中心制造费用率（元 / 工时）
     */
    public record Op(BigDecimal hours, BigDecimal stdHours, BigDecimal goodQty, BigDecimal laborRate, BigDecimal overheadRate) {
    }

    /** 人工、制造费用 */
    public record Amount(BigDecimal labor, BigDecimal overhead) {
        public static final Amount ZERO = new Amount(BigDecimal.ZERO, BigDecimal.ZERO);

        public BigDecimal total() {
            return labor.add(overhead);
        }
    }

    /**
     * 已完工 finishedQty 时工单已发生的工费：有报工工时的工序为全部报工工时的工费，没有的按 完工数量 × 标准工时
     */
    public static Amount incurred(List<Op> ops, BigDecimal finishedQty) {
        return new Amount(money(sum(ops, op -> incurred(op, finishedQty, op.laborRate()))),
                money(sum(ops, op -> incurred(op, finishedQty, op.overheadRate()))));
    }

    /**
     * @param woQty          工单数量
     * @param finishedBefore 本次之前的累计完工数量
     * @param qty            本次完工数量
     * @param absorbed       之前完工入库已转入的工费
     * @return 本次完工入库的工费（2 位小数）
     */
    public static Amount amount(BigDecimal woQty, BigDecimal finishedBefore, BigDecimal qty, List<Op> ops, Amount absorbed) {
        if (ops.isEmpty() || qty.signum() <= 0) {
            return Amount.ZERO;
        }
        BigDecimal after = finishedBefore.add(qty);
        BigDecimal labor = money(sum(ops, op -> incurred(op, after, op.laborRate()))).subtract(absorbed.labor()).max(BigDecimal.ZERO);
        BigDecimal overhead = money(sum(ops, op -> incurred(op, after, op.overheadRate()))).subtract(absorbed.overhead()).max(BigDecimal.ZERO);
        if (after.compareTo(woQty) >= 0 || woQty.signum() <= 0) {
            return new Amount(labor, overhead);
        }
        return new Amount(money(sum(ops, op -> share(op, finishedBefore, qty, op.laborRate()))).min(labor),
                money(sum(ops, op -> share(op, finishedBefore, qty, op.overheadRate()))).min(overhead));
    }

    private static boolean reported(Op op) {
        return op.hours() != null && op.hours().signum() > 0;
    }

    private static BigDecimal incurred(Op op, BigDecimal finishedQty, BigDecimal rate) {
        BigDecimal r = nz(rate);
        return reported(op) ? op.hours().multiply(r) : finishedQty.multiply(nz(op.stdHours())).multiply(r);
    }

    /** 非最后一次完工时某道工序的工费 */
    private static BigDecimal share(Op op, BigDecimal finishedBefore, BigDecimal qty, BigDecimal rate) {
        BigDecimal r = nz(rate);
        if (!reported(op)) {
            return qty.multiply(nz(op.stdHours())).multiply(r);
        }
        BigDecimal good = nz(op.goodQty());
        if (good.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal take = qty.min(good.subtract(finishedBefore).max(BigDecimal.ZERO));
        return take.multiply(op.hours()).multiply(r).divide(good, 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<Op> ops, Function<Op, BigDecimal> f) {
        return ops.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
