package com.lemon.erp.modules.bi.model;

import java.math.BigDecimal;

/**
 * 预警规则的比较方式：指标当前值与阈值比较
 */
public enum AlertOperator {
    /** 大于 */
    GT,
    /** 不小于 */
    GE,
    /** 小于 */
    LT,
    /** 不大于 */
    LE;

    /** 当前值与阈值比较是否满足条件 */
    public boolean test(BigDecimal value, BigDecimal threshold) {
        int c = value.compareTo(threshold);
        return switch (this) {
            case GT -> c > 0;
            case GE -> c >= 0;
            case LT -> c < 0;
            case LE -> c <= 0;
        };
    }
}
