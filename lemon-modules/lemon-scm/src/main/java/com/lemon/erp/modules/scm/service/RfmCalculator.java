package com.lemon.erp.modules.scm.service;

import com.lemon.erp.modules.scm.dto.RfmSettings;
import com.lemon.erp.modules.scm.model.RfmMode;
import com.lemon.erp.modules.scm.model.RfmSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 客户 RFM 打分与分群（进销存报表、客户关系共用的纯计算）。
 * <ul>
 *   <li>得分：R（最近订货天数，越小越好）、F（订货次数）、M（订货金额）各按五分位打 1–5 分——
 *       分数 = ⌈5 × 不优于该客户的客户占比⌉（并列的客户同分，只有一个客户时为 5 分），用于排序和参考</li>
 *   <li>高低：固定阈值模式下，最近订货天数不超过 R 天、每月订货次数（订货次数 ÷ 统计月数）不少于 F 次、
 *       平均每次订货金额（金额 ÷ 订货次数）不少于 M 元为「高」；平均分模式下，某项得分不低于全体平均分为「高」</li>
 *   <li>按 R / F / M 的高低组合分为八类（{@link RfmSegment}）</li>
 * </ul>
 */
public final class RfmCalculator {

    private static final int LEVELS = 5;
    /** 每月平均天数，用于把统计天数折算为月数 */
    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30.4375");

    private RfmCalculator() {
    }

    /**
     * @param recency   最近一次订货距统计截止日的天数
     * @param frequency 订货次数（单据数，大于 0）
     * @param monetary  订货金额（扣除退货）
     */
    public record Metrics(long recency, long frequency, BigDecimal monetary) {
    }

    /**
     * @param monthlyFrequency 每月订货次数（两位小数）
     * @param avgAmount        平均每次订货金额（两位小数）
     * @param recent           R 为「高」
     * @param frequent         F 为「高」
     * @param valuable         M 为「高」
     */
    public record Score(int r, int f, int m, BigDecimal monthlyFrequency, BigDecimal avgAmount,
                        boolean recent, boolean frequent, boolean valuable, RfmSegment segment) {
    }

    /**
     * @param scores 与输入顺序一致
     * @param avgR   R、F、M 得分的平均分（两位小数），没有数据时为 0
     */
    public record Result(List<Score> scores, BigDecimal avgR, BigDecimal avgF, BigDecimal avgM) {
    }

    /** 统计期间的月数（begin 到 asOf 含两端的天数 ÷ 每月平均天数，至少按 1 天计） */
    public static BigDecimal months(LocalDate begin, LocalDate asOf) {
        long days = Math.max(1, ChronoUnit.DAYS.between(begin, asOf) + 1);
        return BigDecimal.valueOf(days).divide(DAYS_PER_MONTH, 6, RoundingMode.HALF_UP);
    }

    /**
     * @param months 统计月数（{@link #months}），用于计算每月订货次数
     */
    public static Result calculate(List<Metrics> rows, RfmSettings settings, BigDecimal months) {
        if (rows.isEmpty()) {
            return new Result(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        // 最近订货天数越小越好：取负数后按「越大越好」打分
        int[] r = scores(rows, x -> BigDecimal.valueOf(-x.recency()));
        int[] f = scores(rows, x -> BigDecimal.valueOf(x.frequency()));
        int[] m = scores(rows, x -> money(x.monetary()));
        double avgR = average(r);
        double avgF = average(f);
        double avgM = average(m);
        boolean threshold = settings.getMode() == RfmMode.THRESHOLD;
        List<Score> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Metrics x = rows.get(i);
            BigDecimal monthly = BigDecimal.valueOf(x.frequency()).divide(months, 2, RoundingMode.HALF_UP);
            BigDecimal avgAmount = x.frequency() <= 0 ? BigDecimal.ZERO
                    : money(x.monetary()).divide(BigDecimal.valueOf(x.frequency()), 2, RoundingMode.HALF_UP);
            boolean recent;
            boolean frequent;
            boolean valuable;
            if (threshold) {
                recent = x.recency() <= settings.getRecencyDays();
                frequent = monthly.compareTo(settings.getFrequency()) >= 0;
                valuable = avgAmount.compareTo(settings.getMonetary()) >= 0;
            } else {
                recent = r[i] >= avgR;
                frequent = f[i] >= avgF;
                valuable = m[i] >= avgM;
            }
            result.add(new Score(r[i], f[i], m[i], monthly, avgAmount, recent, frequent, valuable,
                    RfmSegment.of(recent, frequent, valuable)));
        }
        return new Result(result, round(avgR), round(avgF), round(avgM));
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

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
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
