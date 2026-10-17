package com.lemon.erp.modules.scm.model;

import java.math.BigDecimal;

/**
 * 供应商等级：供应商评估的评定等级（字典 scm_supplier_level）
 */
public enum SupplierLevel {
    /** A 优秀 */
    A,
    /** B 良好 */
    B,
    /** C 合格 */
    C,
    /** D 待改进 */
    D;

    /** 评为 A 级的最低总分 */
    public static final int SCORE_A = 90;
    /** 评为 B 级的最低总分 */
    public static final int SCORE_B = 75;
    /** 评为 C 级（合格）的最低总分 */
    public static final int SCORE_C = 60;

    /** 按评估总分（百分制）评定等级 */
    public static SupplierLevel ofScore(BigDecimal score) {
        double s = score.doubleValue();
        return s >= SCORE_A ? A : s >= SCORE_B ? B : s >= SCORE_C ? C : D;
    }
}
