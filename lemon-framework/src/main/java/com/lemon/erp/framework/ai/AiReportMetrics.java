package com.lemon.erp.framework.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 智能报告快照中的比率计算：增长率、占比都在 Java 中算好放进快照，模型只引用，不自己计算
 */
public final class AiReportMetrics {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AiReportMetrics() {
    }

    /** 增长率（%，保留 1 位小数）：(本期 - 基期) / |基期|，基期为空或 0 时为 null */
    public static BigDecimal growth(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.signum() == 0) {
            return null;
        }
        return current.subtract(base).multiply(HUNDRED).divide(base.abs(), 1, RoundingMode.HALF_UP);
    }

    /** 增长率（%），用于计数 */
    public static BigDecimal growth(long current, long base) {
        return growth(BigDecimal.valueOf(current), BigDecimal.valueOf(base));
    }

    /** 占比（%，保留 1 位小数）：分子 / 分母，分母为空或 0 时为 null */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal percent(long part, long whole) {
        return percent(BigDecimal.valueOf(part), BigDecimal.valueOf(whole));
    }

    /** 空值按 0 */
    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
