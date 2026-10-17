package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.lemon.erp.modules.fin.service.Valuation.money;
import static com.lemon.erp.modules.fin.service.Valuation.unitOf;

/**
 * 先进先出：每次入库形成一个批次，出库先消耗最早的批次；采购退货冲减最近的批次。
 * 批次不够出库时（先出后入），不足部分按最近的单价计价并记为欠数，之后的入库先补欠数
 */
class FifoValuation implements Valuation {

    /** 结存批次（数量可为负，表示期末欠数） */
    static final class Layer {
        final LocalDate date;
        BigDecimal qty;
        BigDecimal amount;

        Layer(LocalDate date, BigDecimal qty, BigDecimal amount) {
            this.date = date;
            this.qty = qty;
            this.amount = amount;
        }
    }

    private final Deque<Layer> layers = new ArrayDeque<>();
    private BigDecimal shortQty = BigDecimal.ZERO;
    private BigDecimal shortAmount = BigDecimal.ZERO;
    private LocalDate shortDate;
    private LocalDate lastDate;
    private BigDecimal lastUnit;

    FifoValuation(List<Layer> opening, BigDecimal fallbackUnit) {
        this.lastUnit = fallbackUnit;
        for (Layer l : opening) {
            if (l.qty.signum() > 0) {
                layers.add(new Layer(l.date, l.qty, l.amount));
                lastUnit = unitOf(l.amount, l.qty);
                lastDate = l.date;
            } else if (l.qty.signum() < 0) {
                shortQty = shortQty.subtract(l.qty);
                shortAmount = shortAmount.subtract(l.amount);
                shortDate = l.date;
            }
        }
    }

    @Override
    public void in(LocalDate date, BigDecimal q, BigDecimal a) {
        if (q.signum() <= 0) {
            return;
        }
        lastUnit = unitOf(a, q);
        lastDate = date;
        if (shortQty.signum() > 0) {
            // 先补欠数：欠数按出库时的单价已计入出库成本，补上后欠数清零，差额由本期出库成本承担
            BigDecimal fill = q.min(shortQty);
            BigDecimal fillAmount = fill.compareTo(q) == 0 ? a : money(a.multiply(fill).divide(q, 10, RoundingMode.HALF_UP));
            BigDecimal shortPart = fill.compareTo(shortQty) == 0 ? shortAmount
                    : money(shortAmount.multiply(fill).divide(shortQty, 10, RoundingMode.HALF_UP));
            shortQty = shortQty.subtract(fill);
            shortAmount = shortAmount.subtract(shortPart);
            q = q.subtract(fill);
            a = a.subtract(fillAmount);
            if (q.signum() == 0) {
                return;
            }
        }
        layers.addLast(new Layer(date, q, a));
    }

    @Override
    public BigDecimal inAtCost(LocalDate date, BigDecimal q) {
        BigDecimal a = money(q.multiply(unit()));
        in(date, q, a);
        return a;
    }

    @Override
    public BigDecimal out(BigDecimal q) {
        BigDecimal cost = BigDecimal.ZERO;
        while (q.signum() > 0 && !layers.isEmpty()) {
            Layer head = layers.peekFirst();
            lastUnit = unitOf(head.amount, head.qty);
            if (head.qty.compareTo(q) <= 0) {
                cost = cost.add(head.amount);
                q = q.subtract(head.qty);
                layers.pollFirst();
            } else {
                BigDecimal part = money(head.amount.multiply(q).divide(head.qty, 10, RoundingMode.HALF_UP));
                cost = cost.add(part);
                head.qty = head.qty.subtract(q);
                head.amount = head.amount.subtract(part);
                q = BigDecimal.ZERO;
            }
        }
        if (q.signum() > 0) {
            BigDecimal part = money(q.multiply(lastUnit));
            cost = cost.add(part);
            shortDate = shortDate != null ? shortDate : lastDate;
            shortQty = shortQty.add(q);
            shortAmount = shortAmount.add(part);
        }
        return cost;
    }

    @Override
    public void returnOut(BigDecimal q, BigDecimal a) {
        BigDecimal total = q;
        BigDecimal left = a;
        while (q.signum() > 0 && !layers.isEmpty()) {
            Layer tail = layers.peekLast();
            BigDecimal take = tail.qty.min(q);
            BigDecimal part = take.compareTo(q) == 0 ? left : money(a.multiply(take).divide(total, 10, RoundingMode.HALF_UP));
            tail.qty = tail.qty.subtract(take);
            tail.amount = tail.amount.subtract(part);
            left = left.subtract(part);
            q = q.subtract(take);
            if (tail.qty.signum() == 0) {
                layers.pollLast();
                // 退货金额与批次金额不一致的差额留在前一个批次
                if (tail.amount.signum() != 0 && !layers.isEmpty()) {
                    layers.peekLast().amount = layers.peekLast().amount.add(tail.amount);
                }
            }
        }
        if (q.signum() > 0) {
            shortQty = shortQty.add(q);
            shortAmount = shortAmount.add(left);
        }
    }

    @Override
    public void adjust(BigDecimal a) {
        if (!layers.isEmpty()) {
            layers.peekLast().amount = layers.peekLast().amount.add(a);
        }
    }

    @Override
    public BigDecimal closeAmount() {
        return Decimals.sum(layers, l -> l.amount).subtract(shortAmount);
    }

    @Override
    public BigDecimal unit() {
        Layer head = layers.peekFirst();
        return head != null ? unitOf(head.amount, head.qty) : lastUnit;
    }

    /** 期末结存批次，欠数为一个数量为负的批次 */
    List<Layer> closingLayers() {
        List<Layer> list = new ArrayList<>(layers);
        if (shortQty.signum() > 0) {
            list.add(new Layer(shortDate != null ? shortDate : lastDate != null ? lastDate : LocalDate.now(),
                    shortQty.negate(), shortAmount.negate()));
        }
        return list;
    }
}
