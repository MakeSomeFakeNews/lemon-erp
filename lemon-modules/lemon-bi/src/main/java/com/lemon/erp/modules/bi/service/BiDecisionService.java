package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.i18n.Messages;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.bi.entity.BiAlertRule;
import com.lemon.erp.modules.bi.mapper.BiAlertRuleMapper;
import com.lemon.erp.modules.bi.mapper.BiInsightMapper;
import com.lemon.erp.modules.bi.model.BiAlertMetric;
import com.lemon.erp.modules.fin.dto.BudgetExecution;
import com.lemon.erp.modules.fin.service.FinBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 决策支持：销售预测（线性趋势 + 季节指数）、ABC 分类、呆滞物料、预警规则
 */
@Service
@RequiredArgsConstructor
public class BiDecisionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** 呆滞判定：超过 90 天没有出库 */
    public static final int SLOW_DAYS = 90;

    private final BiInsightMapper insightMapper;
    private final BiAlertRuleMapper ruleMapper;
    private final FinBudgetService budgetService;

    /**
     * 销售预测：取近 24 个月的月销售额，对最近 12 个月做最小二乘线性回归得到趋势，
     * 有满两年数据时乘以季节指数（同月销售额 / 年均月销售额）；预测未来 months 个月
     *
     * @param category 物料分类，为空预测全部
     */
    /** 驾驶舱、决策分析汇总全公司数据（不按部门拆分），只对有「全部数据」权限的用户开放 */
    static void requireAllData() {
        if (!SecurityUtils.getDataScope().all()) {
            throw new BizException("bi.error.cockpitDecisionAnalysisAggregateCompanyWide");
        }
    }

    public Map<String, Object> forecast(String category, int months) {
        requireAllData();
        if (months < 1 || months > 12) {
            throw new BizException("bi.error.forecastMonthsMust");
        }
        YearMonth thisMonth = YearMonth.now();
        YearMonth start = thisMonth.minusMonths(23);
        Map<Integer, Map<String, Object>> rows = BiCockpitService.byPeriod(insightMapper.selectSalesByPeriod(BiCockpitService.period(start), category));
        // 本月未结束，只用到上月为止的完整月份
        List<BigDecimal> series = new ArrayList<>();
        List<Map<String, Object>> history = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            YearMonth ym = start.plusMonths(i);
            BigDecimal v = BiCockpitService.value(rows, BiCockpitService.period(ym), "amount");
            if (ym.isBefore(thisMonth)) {
                series.add(v);
            }
            history.add(Map.of("period", BiCockpitService.period(ym), "amount", v));
        }
        int n = Math.min(12, series.size());
        List<BigDecimal> recent = series.subList(series.size() - n, series.size());
        // y = a + b * x，x 为 0..n-1
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int x = 0; x < n; x++) {
            double y = recent.get(x).doubleValue();
            sx += x;
            sy += y;
            sxy += x * y;
            sxx += x * x;
        }
        double b = n * sxx - sx * sx == 0 ? 0 : (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double a = (sy - b * sx) / n;
        boolean seasonal = series.stream().limit(series.size() - 12L).anyMatch(v -> v.signum() > 0) && series.size() >= 23;
        double yearAvg = seasonal ? series.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0) : 0;
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            YearMonth ym = thisMonth.plusMonths(i);
            double trend = a + b * (n + i);
            double index = 1;
            if (seasonal && yearAvg > 0) {
                // 季节指数：前两年同月的平均 / 两年月均
                double same = 0;
                int cnt = 0;
                for (int k = 0; k < series.size(); k++) {
                    if (start.plusMonths(k).getMonthValue() == ym.getMonthValue()) {
                        same += series.get(k).doubleValue();
                        cnt++;
                    }
                }
                index = cnt == 0 || same == 0 ? 1 : (same / cnt) / yearAvg;
            }
            forecast.add(Map.of("period", BiCockpitService.period(ym),
                    "amount", BigDecimal.valueOf(Math.max(0, trend * index)).setScale(2, RoundingMode.HALF_UP)));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("history", history);
        result.put("forecast", forecast);
        result.put("slope", BigDecimal.valueOf(b).setScale(2, RoundingMode.HALF_UP));
        result.put("seasonal", seasonal);
        result.put("method", seasonal ? Messages.get("bi.text.linearTrendSeasonalIndex") : Messages.get("bi.text.linearTrendLastMonths", n));
        return result;
    }

    /**
     * ABC 分类：近一年各物料的销售成本（没有成本时用销售额）降序累计，累计占比 70% 以内 A 类、90% 以内 B 类，其余 C 类
     */
    public Map<String, Object> abc() {
        requireAllData();
        List<Map<String, Object>> src = insightMapper.selectAbcSource(LocalDate.now().minusYears(1));
        boolean useCost = src.stream().anyMatch(r -> dec(r.get("cost")).signum() > 0);
        String key = useCost ? "cost" : "amount";
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : src) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("value", dec(r.get(key)));
            rows.add(m);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (BigDecimal) r.get("value")).reversed());
        BigDecimal total = rows.stream().map(r -> (BigDecimal) r.get("value")).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cum = BigDecimal.ZERO;
        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (String c : List.of("A", "B", "C")) {
            counts.put(c, new long[]{0});
            values.put(c, BigDecimal.ZERO);
        }
        for (Map<String, Object> r : rows) {
            BigDecimal v = (BigDecimal) r.get("value");
            BigDecimal before = total.signum() == 0 ? BigDecimal.ZERO : cum.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            cum = cum.add(v);
            BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO : cum.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            // 按进入该物料前的累计占比分类，保证第一个物料总是 A 类
            String cls = before.compareTo(BigDecimal.valueOf(70)) < 0 ? "A" : before.compareTo(BigDecimal.valueOf(90)) < 0 ? "B" : "C";
            r.put("share", total.signum() == 0 ? BigDecimal.ZERO : v.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP));
            r.put("cumulative", pct);
            r.put("class", cls);
            counts.get(cls)[0]++;
            values.merge(cls, v, BigDecimal::add);
        }
        List<Map<String, Object>> summary = new ArrayList<>();
        counts.forEach((c, cnt) -> summary.add(Map.of("class", c, "count", cnt[0], "value", values.get(c),
                "share", total.signum() == 0 ? BigDecimal.ZERO : values.get(c).multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP))));
        return Map.of("rows", rows, "summary", summary, "basis", useCost ? Messages.get("bi.text.costSales") : Messages.get("bi.text.sales"), "total", total);
    }

    /** 呆滞物料：有库存但超过 days 天没有出库 */
    public List<Map<String, Object>> slowMoving(int days) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : insightMapper.selectSlowMoving(now.minusDays(days))) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            Object since = r.get("lastOut") != null ? r.get("lastOut") : r.get("firstIn");
            m.put("idleDays", since == null ? null : ChronoUnit.DAYS.between(LocalDateTime.parse(since.toString().replace(' ', 'T').substring(0, 19)), now));
            rows.add(m);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> dec(r.get("amount"))).reversed());
        return rows;
    }

    // ---------------------------------------------------------------- 预警

    public List<Map<String, Object>> metrics() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BiAlertMetric m : BiAlertMetric.values()) {
            list.add(Map.of("key", m.name(), "label", Messages.get(m.getLabel()), "unit", Messages.get(m.getUnit())));
        }
        return list;
    }

    public List<BiAlertRule> rules() {
        return ruleMapper.selectList(Wrappers.<BiAlertRule>lambdaQuery().orderByAsc(BiAlertRule::getId));
    }

    public void saveRule(Long id, BiAlertRule rule) {
        if (BiAlertMetric.of(rule.getMetric()) == null) {
            throw new BizException("bi.error.invalidMetric");
        }
        if (rule.getEnabled() == null) rule.setEnabled(1);
        rule.setId(id);
        if (id == null) {
            ruleMapper.insert(rule);
        } else {
            if (ruleMapper.selectById(id) == null) {
                throw new BizException("bi.error.alertRuleNotFound");
            }
            ruleMapper.updateById(rule);
        }
    }

    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    /**
     * 计算所有启用规则的指标值，返回每条规则的当前值与是否触发
     */
    public List<Map<String, Object>> alerts() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        int period = today.getYear() * 100 + today.getMonthValue();
        Map<BiAlertMetric, BigDecimal> cache = new LinkedHashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BiAlertRule rule : rules()) {
            if (!Integer.valueOf(1).equals(rule.getEnabled())) {
                continue;
            }
            BiAlertMetric metric = BiAlertMetric.of(rule.getMetric());
            if (metric == null) {
                continue;
            }
            BigDecimal value = cache.computeIfAbsent(metric, m -> m == BiAlertMetric.BUDGET_OVERRUN ? budgetOverrun(today)
                    : insightMapper.selectMetric(m.name(), today, now, period, now.minusDays(SLOW_DAYS)));
            boolean triggered = value != null && switch (rule.getOperator()) {
                case "GT" -> value.compareTo(rule.getThreshold()) > 0;
                case "GE" -> value.compareTo(rule.getThreshold()) >= 0;
                case "LT" -> value.compareTo(rule.getThreshold()) < 0;
                default -> value.compareTo(rule.getThreshold()) <= 0;
            };
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ruleId", rule.getId());
            m.put("name", rule.getName());
            m.put("metric", metric.name());
            m.put("metricLabel", Messages.get(metric.getLabel()));
            m.put("unit", Messages.get(metric.getUnit()));
            m.put("operator", rule.getOperator());
            m.put("threshold", rule.getThreshold());
            m.put("level", rule.getLevel());
            m.put("value", value == null ? null : value.setScale(2, RoundingMode.HALF_UP));
            m.put("triggered", triggered);
            m.put("remark", rule.getRemark());
            result.add(m);
        }
        return result;
    }

    /** 本年累计（截至本月）执行数超过累计预算的预算科目数 */
    private BigDecimal budgetOverrun(LocalDate today) {
        int month = today.getMonthValue();
        long count = 0;
        for (BudgetExecution e : budgetService.execution(today.getYear(), null)) {
            BigDecimal budget = BigDecimal.ZERO;
            BigDecimal actual = BigDecimal.ZERO;
            for (int i = 0; i < month; i++) {
                budget = budget.add(e.budgets().get(i));
                actual = actual.add(e.actuals().get(i));
            }
            if (budget.signum() > 0 && actual.compareTo(budget) > 0) {
                count++;
            }
        }
        return BigDecimal.valueOf(count);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
