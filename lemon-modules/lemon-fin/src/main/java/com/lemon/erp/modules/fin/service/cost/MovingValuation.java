package com.lemon.erp.modules.fin.service.cost;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.lemon.erp.modules.fin.service.cost.Valuation.money;
import static com.lemon.erp.modules.fin.service.cost.Valuation.unitOf;

/**
 * 移动加权平均：每次入库后重算平均单价，出库按当时的平均单价计价；结存数量为 0 时出库全部结存金额。
 * 结存为负（先出后入）时按最近的单价计价
 */
class MovingValuation implements Valuation {

    private BigDecimal qty;
    private BigDecimal amount;
    private BigDecimal lastUnit;

    MovingValuation(BigDecimal openQty, BigDecimal openAmount, BigDecimal fallbackUnit) {
        this.qty = openQty;
        this.amount = openAmount;
        this.lastUnit = openQty.signum() > 0 ? unitOf(openAmount, openQty) : fallbackUnit;
    }

    @Override
    public void in(LocalDate date, BigDecimal q, BigDecimal a) {
        qty = qty.add(q);
        amount = amount.add(a);
        refreshUnit();
    }

    @Override
    public BigDecimal inAtCost(LocalDate date, BigDecimal q) {
        BigDecimal a = money(q.multiply(unit()));
        in(date, q, a);
        return a;
    }

    @Override
    public BigDecimal out(BigDecimal q) {
        BigDecimal cost;
        if (qty.signum() > 0 && q.compareTo(qty) >= 0) {
            // 全部出库：结存金额全部转出，超出部分按最近单价
            cost = amount.add(money(q.subtract(qty).multiply(lastUnit)));
        } else {
            cost = money(q.multiply(unit()));
        }
        qty = qty.subtract(q);
        amount = amount.subtract(cost);
        refreshUnit();
        return cost;
    }

    @Override
    public void returnOut(BigDecimal q, BigDecimal a) {
        qty = qty.subtract(q);
        amount = amount.subtract(a);
        refreshUnit();
    }

    @Override
    public void adjust(BigDecimal a) {
        amount = amount.add(a);
        refreshUnit();
    }

    @Override
    public BigDecimal closeAmount() {
        return amount;
    }

    @Override
    public BigDecimal unit() {
        return qty.signum() > 0 && amount.signum() >= 0 ? unitOf(amount, qty) : lastUnit;
    }

    private void refreshUnit() {
        if (qty.signum() > 0 && amount.signum() >= 0) {
            lastUnit = unitOf(amount, qty);
        }
    }
}
