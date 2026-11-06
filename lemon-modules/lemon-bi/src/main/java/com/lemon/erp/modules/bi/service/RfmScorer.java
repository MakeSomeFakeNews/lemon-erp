package com.lemon.erp.modules.bi.service;

import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.model.RfmSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * RFM 打分：R（最近购买天数，越小越好）、F（单据数）、M（净销售额）各按五分位打 1–5 分——
 * 分数 = ⌈5 × 不优于该客户的客户占比⌉（并列的客户同分，只有一个客户时为 5 分）；
 * 某项得分不低于全体平均分为「高」，按 R / F / M 的高低组合分为八类（{@link RfmSegment}）
 */
public final class RfmScorer {

    private static final int LEVELS = 5;

    private RfmScorer() {
    }

    /**
     * 计算各行的 r、f、m、segment（原地修改）
     *
     * @return 平均分 [R, F, M]（两位小数），没有数据时为 0
     */
    public static BigDecimal[] score(List<JxcViews.RfmRow> rows) {
        if (rows.isEmpty()) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }
        // 最近购买天数越小越好：取负数后按「越大越好」打分
        int[] r = scores(rows, x -> BigDecimal.valueOf(-x.getRecency()));
        int[] f = scores(rows, x -> BigDecimal.valueOf(x.getFrequency()));
        int[] m = scores(rows, x -> x.getMonetary() == null ? BigDecimal.ZERO : x.getMonetary());
        double avgR = average(r);
        double avgF = average(f);
        double avgM = average(m);
        for (int i = 0; i < rows.size(); i++) {
            JxcViews.RfmRow row = rows.get(i);
            row.setR(r[i]);
            row.setF(f[i]);
            row.setM(m[i]);
            row.setSegment(RfmSegment.of(r[i] >= avgR, f[i] >= avgF, m[i] >= avgM));
        }
        return new BigDecimal[]{round(avgR), round(avgF), round(avgM)};
    }

    /** 越大越好的五分位得分：⌈5 × (取值不大于它的个数) / n⌉ */
    static <T> int[] scores(List<T> rows, Function<T, BigDecimal> value) {
        int n = rows.size();
        List<BigDecimal> sorted = new ArrayList<>(n);
        rows.forEach(x -> sorted.add(value.apply(x)));
        Collections.sort(sorted);
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            int notGreater = upperBound(sorted, value.apply(rows.get(i)));
            result[i] = Math.max(1, (int) Math.ceil((double) LEVELS * notGreater / n));
        }
        return result;
    }

    /** 有序列表中不大于 v 的元素个数 */
    private static int upperBound(List<BigDecimal> sorted, BigDecimal v) {
        int lo = 0;
        int hi = sorted.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted.get(mid).compareTo(v) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static double average(int[] values) {
        double sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
