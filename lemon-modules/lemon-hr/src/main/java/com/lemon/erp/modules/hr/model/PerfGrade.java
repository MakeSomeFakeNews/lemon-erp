package com.lemon.erp.modules.hr.model;

import java.math.BigDecimal;

/**
 * 绩效等级：绩效等级与奖金系数（字典 hr_perf_grade）。90 分及以上 S，80 A，70 B，60 C，其余 D
 */
public enum PerfGrade {
    /** S 卓越 */
    S(90, new BigDecimal("1.5")),
    /** A 优秀 */
    A(80, new BigDecimal("1.2")),
    /** B 良好 */
    B(70, BigDecimal.ONE),
    /** C 合格 */
    C(60, new BigDecimal("0.8")),
    /** D 待改进 */
    D(0, BigDecimal.ZERO);

    /** 达到该等级的最低分 */
    private final int minScore;
    /** 绩效奖金系数 */
    private final BigDecimal bonusCoefficient;

    PerfGrade(int minScore, BigDecimal bonusCoefficient) {
        this.minScore = minScore;
        this.bonusCoefficient = bonusCoefficient;
    }

    public BigDecimal getBonusCoefficient() {
        return bonusCoefficient;
    }

    /** 按考核得分确定等级 */
    public static PerfGrade of(BigDecimal score) {
        double s = score.doubleValue();
        for (PerfGrade g : values()) {
            if (s >= g.minScore) {
                return g;
            }
        }
        return D;
    }

    /** 绩效奖金系数，没有等级（未参加考核）时为 0 */
    public static BigDecimal coefficientOf(PerfGrade grade) {
        return grade == null ? BigDecimal.ZERO : grade.bonusCoefficient;
    }
}
