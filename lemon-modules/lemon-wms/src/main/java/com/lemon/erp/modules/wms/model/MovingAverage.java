package com.lemon.erp.modules.wms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 移动加权平均的计算（纯函数）：按物料维护 数量 / 金额 / 单价，每次入库、出库后重新计算单价。
 * <ul>
 *   <li>入库：按指定单价（采购不含税单价、填写的成本价等）计价，没有指定时按当前均价；单价 = 金额 / 数量</li>
 *   <li>出库：按当前均价计价（指定单价的如采购退货按指定单价，但不超过剩余金额）；出完时剩余金额全部转出，不留尾差</li>
 *   <li>数量为 0 时保留最后的单价，下次入库没有指定单价时按它计价；从未有过单价时取参考成本价</li>
 * </ul>
 */
public final class MovingAverage {

    /** 单价保留的小数位 */
    public static final int PRICE_SCALE = 6;

    private MovingAverage() {
    }

    /** 某物料的成本状态 */
    public record State(BigDecimal qty, BigDecimal amount, BigDecimal unitCost) {
        public static final State EMPTY = new State(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * 一次变动的结果
     *
     * @param next     变动后的状态
     * @param unitCost 本次变动的成本单价
     * @param amount   本次变动的成本金额，与数量同号
     */
    public record Result(State next, BigDecimal unitCost, BigDecimal amount) {
    }

    /**
     * 当前均价：有库存时为单价；没有库存且没有单价时取参考成本价
     *
     * @param refCost 物料的参考成本价，可为空
     */
    public static BigDecimal average(State s, BigDecimal refCost) {
        if (s.unitCost().signum() > 0 || s.qty().signum() > 0) {
            return s.unitCost();
        }
        return refCost == null ? BigDecimal.ZERO : refCost;
    }

    /**
     * @param qty       变动数量，正数入库、负数出库
     * @param specified 指定的单价，为空按当前均价
     * @param refCost   参考成本价，可为空
     */
    public static Result apply(State s, BigDecimal qty, BigDecimal specified, BigDecimal refCost) {
        BigDecimal avg = average(s, refCost);
        BigDecimal cost = specified == null ? avg : specified;
        if (qty.signum() >= 0) {
            BigDecimal amount = money(qty.multiply(cost));
            BigDecimal nq = s.qty().add(qty);
            BigDecimal na = s.amount().add(amount);
            return new Result(state(nq, na, cost), cost, amount);
        }
        BigDecimal out = qty.negate();
        BigDecimal nq = s.qty().subtract(out);
        BigDecimal amount;
        if (nq.signum() <= 0) {
            // 出完：剩余金额全部转出；数量账实不符（成本数量少于出库数量）时超出部分按单价计价
            amount = s.amount().add(money(nq.negate().multiply(cost)));
        } else {
            amount = money(out.multiply(cost));
            if (amount.compareTo(s.amount()) > 0 && s.amount().signum() >= 0) {
                amount = s.amount();
            }
        }
        BigDecimal na = nq.signum() <= 0 ? BigDecimal.ZERO : s.amount().subtract(amount);
        BigDecimal unit = out.signum() == 0 ? cost : amount.divide(out, PRICE_SCALE, RoundingMode.HALF_UP);
        return new Result(state(nq, na, s.unitCost().signum() > 0 || s.qty().signum() > 0 ? s.unitCost() : avg), unit, amount.negate());
    }

    /** 变动后的状态：有库存时单价 = 金额 / 数量，否则保留 fallback */
    private static State state(BigDecimal qty, BigDecimal amount, BigDecimal fallback) {
        BigDecimal unit = qty.signum() > 0 ? amount.divide(qty, PRICE_SCALE, RoundingMode.HALF_UP) : fallback;
        return new State(qty, amount, unit.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    }

    /** 不指定单价、只按当前均价计价的金额（调拨、查询） */
    public static BigDecimal value(BigDecimal qty, BigDecimal unitCost) {
        return money(qty.multiply(unitCost));
    }

    /**
     * 把金额按权重分摊到各行，返回各行的单价（金额 / 数量）；最后一行取尾差，各行金额合计等于总金额。
     * 权重合计为 0 时按数量分摊
     */
    public static List<BigDecimal> allocate(BigDecimal total, List<BigDecimal> qtys, List<BigDecimal> weights) {
        BigDecimal sum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> w = sum.signum() > 0 ? weights : qtys;
        BigDecimal base = sum.signum() > 0 ? sum : qtys.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> result = new ArrayList<>(qtys.size());
        BigDecimal rest = total;
        for (int i = 0; i < qtys.size(); i++) {
            BigDecimal amount = i == qtys.size() - 1 ? rest
                    : base.signum() == 0 ? BigDecimal.ZERO : money(total.multiply(w.get(i)).divide(base, 10, RoundingMode.HALF_UP));
            rest = rest.subtract(amount);
            result.add(qtys.get(i).signum() == 0 ? BigDecimal.ZERO : amount.divide(qtys.get(i), PRICE_SCALE, RoundingMode.HALF_UP));
        }
        return result;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
