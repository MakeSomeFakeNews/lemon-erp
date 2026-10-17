package com.lemon.erp.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/**
 * 金额、数量的常用计算。精度约定与数据库一致：金额 2 位、数量和单价 4 位，四舍五入（HALF_UP）
 */
public final class Decimals {

    public static final int MONEY_SCALE = 2;
    public static final int QTY_SCALE = 4;
    public static final int PRICE_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Decimals() {
    }

    /** null 视为 0 */
    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 求和（null 视为 0） */
    public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (T i : items) {
                total = total.add(nz(getter.apply(i)));
            }
        }
        return total;
    }

    /** 求和（null 视为 0），如 map.values() */
    public static BigDecimal sum(Collection<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        if (values != null) {
            for (BigDecimal v : values) {
                total = total.add(nz(v));
            }
        }
        return total;
    }

    /** 金额：保留 2 位 */
    public static BigDecimal money(BigDecimal v) {
        return nz(v).setScale(MONEY_SCALE, ROUNDING);
    }

    /** 数量：保留 4 位 */
    public static BigDecimal qty(BigDecimal v) {
        return nz(v).setScale(QTY_SCALE, ROUNDING);
    }

    /** part / whole × 100，保留 scale 位；whole 为 0 或 null 时返回 null */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole, int scale) {
        if (whole == null || whole.signum() == 0) {
            return null;
        }
        return nz(part).multiply(HUNDRED).divide(whole, scale, ROUNDING);
    }

    /** a 是否大于 0（null 视为 0） */
    public static boolean positive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }
}
