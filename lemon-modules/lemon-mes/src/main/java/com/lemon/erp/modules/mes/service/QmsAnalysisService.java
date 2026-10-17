package com.lemon.erp.modules.mes.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.modules.mes.dto.QmsViews;
import com.lemon.erp.modules.mes.entity.QmsInspectionItem;
import com.lemon.erp.modules.mes.mapper.QmsAnalysisMapper;
import com.lemon.erp.modules.mes.model.SpcChart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 质量分析：合格率趋势、不良柏拉图、SPC（I-MR / X̄-R / X̄-S 控制图与过程能力指数）
 */
@Service
@RequiredArgsConstructor
public class QmsAnalysisService {

    /** 控制图常数，下标为子组大小 n（2 ~ 10） */
    private static final double[] A2 = {0, 0, 1.880, 1.023, 0.729, 0.577, 0.483, 0.419, 0.373, 0.337, 0.308};
    private static final double[] D3 = {0, 0, 0, 0, 0, 0, 0, 0.076, 0.136, 0.184, 0.223};
    private static final double[] D4 = {0, 0, 3.267, 2.574, 2.282, 2.114, 2.004, 1.924, 1.864, 1.816, 1.777};
    private static final double[] D2 = {0, 0, 1.128, 1.693, 2.059, 2.326, 2.534, 2.704, 2.847, 2.970, 3.078};
    /** X̄-R 图的最大子组大小，更大的子组用 X̄-S 图 */
    private static final int MAX_R_N = 10;
    /** 默认统计近 12 个月 */
    private static final int DEFAULT_MONTHS = 12;

    private final QmsAnalysisMapper analysisMapper;

    public List<QmsViews.TrendRow> trend(LocalDate begin, LocalDate end) {
        LocalDate e = end != null ? end : LocalDate.now();
        LocalDate b = begin != null ? begin : e.minusMonths(DEFAULT_MONTHS - 1).withDayOfMonth(1);
        return analysisMapper.selectTrend(b, e);
    }

    public List<QmsViews.ParetoRow> pareto(LocalDate begin, LocalDate end) {
        LocalDate e = end != null ? end : LocalDate.now();
        LocalDate b = begin != null ? begin : e.minusMonths(DEFAULT_MONTHS - 1).withDayOfMonth(1);
        return analysisMapper.selectPareto(b, e);
    }

    public List<QmsViews.SpcItem> spcItems() {
        return analysisMapper.selectSpcItems();
    }

    /**
     * 每张检验单该项目的实测值为一个子组。子组大小取各子组样本数的众数，其他大小的子组不参与计算；
     * 子组大小为 1 用单值-移动极差（I-MR）图，2 ~ 10 用均值-极差（X̄-R）图，大于 10 用均值-标准差（X̄-S）图
     */
    public QmsViews.Spc spc(Long planItemId, LocalDate begin, LocalDate end) {
        QmsInspectionItem spec = analysisMapper.selectPlanItemSpec(planItemId);
        if (spec == null) {
            throw new BizException("检验项目不存在");
        }
        List<QmsAnalysisMapper.SpcRow> rows = analysisMapper.selectSpcRows(planItemId, begin, end);
        List<double[]> groups = rows.stream().map(r -> parse(r.measured())).toList();
        int n = groups.stream().filter(g -> g.length > 0)
                .collect(Collectors.groupingBy(g -> g.length, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).orElse(0);
        SpcChart chart = n <= 1 ? SpcChart.I_MR : n <= MAX_R_N ? SpcChart.XBAR_R : SpcChart.XBAR_S;
        List<QmsViews.SpcPoint> points = new ArrayList<>();
        List<Double> means = new ArrayList<>();
        List<Double> spreads = new ArrayList<>();
        Double prev = null;
        for (int i = 0; i < rows.size(); i++) {
            double[] g = groups.get(i);
            if (n == 0 || g.length != n) {
                continue;
            }
            double mean = Arrays.stream(g).average().orElse(0);
            Double spread = switch (chart) {
                case I_MR -> prev == null ? null : Math.abs(mean - prev);
                case XBAR_R -> max(g) - min(g);
                case XBAR_S -> stdDev(g, mean);
            };
            prev = mean;
            QmsAnalysisMapper.SpcRow r = rows.get(i);
            points.add(new QmsViews.SpcPoint(r.inspectionId(), r.inspNo(), r.inspectDate(), n, dec(mean, 4),
                    spread == null ? null : dec(spread, 4)));
            means.add(mean);
            if (spread != null) {
                spreads.add(spread);
            }
        }
        if (points.size() < 2 || spreads.isEmpty()) {
            return new QmsViews.Spc(spec.getName(), spec.getUnit(), spec.getLowerLimit(), spec.getUpperLimit(), n, chart, points,
                    null, null, null, null, null, null, null, null, null, List.of(I18n.t("数据不足：至少需要 2 个相同大小的子组")));
        }
        double center = means.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double spreadCenter = spreads.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        // 均值图控制限 = 中心线 ± k × 离散中心线；离散图控制限 = 下限系数、上限系数 × 离散中心线；σ = 离散中心线 / 系数
        double k;
        double lower;
        double upper;
        double sigma;
        if (chart == SpcChart.XBAR_S) {
            double c4 = c4(n);
            double w = 3 * Math.sqrt(1 - c4 * c4) / c4;
            k = 3 / (c4 * Math.sqrt(n));
            lower = Math.max(0, 1 - w);
            upper = 1 + w;
            sigma = spreadCenter / c4;
        } else {
            // 单值图用 n = 2 的常数（移动极差为相邻两点之差）
            int m = Math.max(n, 2);
            k = chart == SpcChart.I_MR ? 3 / D2[2] : A2[m];
            lower = D3[m];
            upper = D4[m];
            sigma = spreadCenter / D2[m];
        }
        double meanUcl = center + k * spreadCenter;
        double meanLcl = center - k * spreadCenter;
        double spreadUcl = upper * spreadCenter;
        double spreadLcl = lower * spreadCenter;

        BigDecimal cp = null;
        BigDecimal cpk = null;
        Double usl = spec.getUpperLimit() == null ? null : spec.getUpperLimit().doubleValue();
        Double lsl = spec.getLowerLimit() == null ? null : spec.getLowerLimit().doubleValue();
        if (sigma > 0) {
            if (usl != null && lsl != null) {
                cp = dec((usl - lsl) / (6 * sigma), 2);
            }
            Double cpu = usl == null ? null : (usl - center) / (3 * sigma);
            Double cpl = lsl == null ? null : (center - lsl) / (3 * sigma);
            if (cpu != null || cpl != null) {
                cpk = dec(cpu == null ? cpl : cpl == null ? cpu : Math.min(cpu, cpl), 2);
            }
        }
        String spreadName = switch (chart) {
            case I_MR -> I18n.t("移动极差");
            case XBAR_R -> I18n.t("极差");
            case XBAR_S -> I18n.t("标准差");
        };
        List<String> signals = new ArrayList<>();
        for (QmsViews.SpcPoint p : points) {
            double m = p.mean().doubleValue();
            if (m > meanUcl || m < meanLcl) {
                String value = p.mean().stripTrailingZeros().toPlainString();
                signals.add(chart == SpcChart.I_MR ? I18n.t("{0} 单值 {1} 超出控制限", p.inspNo(), value) : I18n.t("{0} 均值 {1} 超出控制限", p.inspNo(), value));
            }
            if (p.spread() != null && (p.spread().doubleValue() > spreadUcl || p.spread().doubleValue() < spreadLcl)) {
                signals.add(I18n.t("{0} {1} {2} 超出控制限", p.inspNo(), spreadName, p.spread().stripTrailingZeros().toPlainString()));
            }
        }
        return new QmsViews.Spc(spec.getName(), spec.getUnit(), spec.getLowerLimit(), spec.getUpperLimit(), n, chart, points,
                dec(center, 4), dec(meanUcl, 4), dec(meanLcl, 4), dec(spreadCenter, 4), dec(spreadUcl, 4), dec(spreadLcl, 4),
                dec(sigma, 4), cp, cpk, signals);
    }

    // ---------------------------------------------------------------- 内部

    private static double[] parse(String measured) {
        if (measured == null || measured.isBlank()) {
            return new double[0];
        }
        return Arrays.stream(measured.split(",")).map(String::trim).filter(s -> !s.isEmpty()).mapToDouble(Double::parseDouble).toArray();
    }

    /** 样本标准差（n - 1） */
    private static double stdDev(double[] values, double mean) {
        double sum = Arrays.stream(values).map(v -> (v - mean) * (v - mean)).sum();
        return Math.sqrt(sum / (values.length - 1));
    }

    /**
     * 无偏常数 c₄ = √(2 / (n - 1)) · Γ(n / 2) / Γ((n - 1) / 2)。Γ 的比值 r(n) 按递推计算避免溢出：
     * r(2) = 1 / √π，r(n + 1) = ((n - 1) / 2) / r(n)
     */
    static double c4(int n) {
        double r = 1 / Math.sqrt(Math.PI);
        for (int i = 2; i < n; i++) {
            r = ((i - 1) / 2.0) / r;
        }
        return Math.sqrt(2.0 / (n - 1)) * r;
    }

    private static double max(double[] values) {
        return Arrays.stream(values).max().orElse(0);
    }

    private static double min(double[] values) {
        return Arrays.stream(values).min().orElse(0);
    }

    private static BigDecimal dec(double v, int scale) {
        return new BigDecimal(v, MathContext.DECIMAL64).setScale(scale, RoundingMode.HALF_UP);
    }
}
