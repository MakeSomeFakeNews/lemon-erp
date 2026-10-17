package com.lemon.erp.modules.fin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 成本核算中按发生顺序逐笔计价的存货（移动加权平均、先进先出），金额保留 2 位小数
 */
interface Valuation {

    /** 按给定金额入库（采购入库、完工入库） */
    void in(LocalDate date, BigDecimal qty, BigDecimal amount);

    /** 按当前成本入库（其他入库、退料、销售退货），返回入库金额 */
    BigDecimal inAtCost(LocalDate date, BigDecimal qty);

    /** 出库，返回出库成本 */
    BigDecimal out(BigDecimal qty);

    /** 采购退货：按退货的不含税金额冲减 */
    void returnOut(BigDecimal qty, BigDecimal amount);

    /** 调整结存金额（尾差） */
    void adjust(BigDecimal amount);

    /** 期末结存金额 */
    BigDecimal closeAmount();

    /** 当前单价：下一笔出库的计价单价 */
    BigDecimal unit();

    static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal unitOf(BigDecimal amount, BigDecimal qty) {
        return amount.divide(qty, 6, RoundingMode.HALF_UP);
    }
}
