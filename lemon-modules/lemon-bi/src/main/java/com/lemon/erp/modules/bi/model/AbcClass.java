package com.lemon.erp.modules.bi.model;

import java.math.BigDecimal;

/**
 * ABC 分类：按销售成本（或销售额）降序累计占比分类
 */
public enum AbcClass {
    /** 累计占比 70% 以内 */
    A,
    /** 累计占比 90% 以内 */
    B,
    /** 其余 */
    C;

    /** A 类累计占比上限（%） */
    private static final BigDecimal A_LIMIT = BigDecimal.valueOf(70);
    /** B 类累计占比上限（%） */
    private static final BigDecimal B_LIMIT = BigDecimal.valueOf(90);

    /** 按进入该物料前的累计占比（%）分类，保证第一个物料总是 A 类 */
    public static AbcClass of(BigDecimal cumulativeBefore) {
        return cumulativeBefore.compareTo(A_LIMIT) < 0 ? A : cumulativeBefore.compareTo(B_LIMIT) < 0 ? B : C;
    }
}
