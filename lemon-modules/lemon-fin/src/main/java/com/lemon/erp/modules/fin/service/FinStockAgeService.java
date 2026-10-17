package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.modules.fin.mapper.FinStockAgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;

/**
 * 库龄分析：截至日期的结存按先进先出对应到最近的入库记录（结存来自最后几次入库），按入库距今天数分段统计数量和金额。
 * 不按仓库时统计全部仓库（调拨不改变库龄）；按仓库时调入视为入库。金额 = 数量 × 截至期间最近一次核算的单价（没有时取参考成本价）
 */
@Service
@RequiredArgsConstructor
public class FinStockAgeService {

    /** 默认分段（天）：0-30、31-60、61-90、91-180、181-365、365 以上 */
    private static final List<Integer> DEFAULT_BUCKETS = List.of(30, 60, 90, 180, 365);
    private static final int MAX_BUCKETS = 8;
    private static final int CHUNK = 500;

    private final FinStockAgeMapper ageMapper;

    /**
     * @param buckets 分段上限（天，递增），为空用默认
     * @return {buckets: [{label}], rows: [{materialId, code, name, spec, unit, category, qty, amount, unitCost, oldestDate,
     * ages: [{qty, amount}]}], total: {qty, amount, ages}}
     */
    public Map<String, Object> analyze(LocalDate asOf, Long warehouseId, String category, String keyword, List<Integer> buckets) {
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        List<Integer> limits = buckets == null || buckets.isEmpty() ? DEFAULT_BUCKETS : buckets;
        for (int i = 0; i < limits.size(); i++) {
            if (limits.get(i) <= 0 || (i > 0 && limits.get(i) <= limits.get(i - 1))) {
                throw new BizException("库龄分段必须是递增的正整数");
            }
        }
        if (limits.size() > MAX_BUCKETS) {
            throw new BizException("库龄最多分 {0} 段", MAX_BUCKETS);
        }
        List<Map<String, Object>> labels = new ArrayList<>();
        for (int i = 0; i <= limits.size(); i++) {
            String label = i == 0 ? I18n.t("{0}-{1} 天", 0, limits.get(0))
                    : i == limits.size() ? I18n.t("{0} 天以上", limits.get(i - 1)) : I18n.t("{0}-{1} 天", limits.get(i - 1) + 1, limits.get(i));
            labels.add(Map.of("label", label));
        }
        List<Map<String, Object>> balances = ageMapper.selectBalances(date, warehouseId, category, keyword);
        List<Long> ids = balances.stream().map(r -> ((Number) r.get("materialId")).longValue()).toList();
        Map<Long, List<Map<String, Object>>> inflows = new HashMap<>();
        Map<Long, BigDecimal> units = new HashMap<>();
        for (int i = 0; i < ids.size(); i += CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + CHUNK, ids.size()));
            ageMapper.selectInflows(date, warehouseId, chunk).forEach(r ->
                    inflows.computeIfAbsent(((Number) r.get("materialId")).longValue(), k -> new ArrayList<>()).add(r));
            ageMapper.selectUnitCosts(periodOf(date), chunk).forEach(r -> units.put(((Number) r.get("materialId")).longValue(), dec(r.get("unitCost"))));
        }
        int n = limits.size() + 1;
        BigDecimal[] totalQty = zeros(n);
        BigDecimal[] totalAmount = zeros(n);
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumAmount = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> b : balances) {
            Long id = ((Number) b.get("materialId")).longValue();
            BigDecimal qty = dec(b.get("qty"));
            BigDecimal unit = units.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal[] ageQty = zeros(n);
            BigDecimal remaining = qty;
            LocalDate oldest = null;
            for (Map<String, Object> in : inflows.getOrDefault(id, List.of())) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal take = remaining.min(dec(in.get("qty")));
                LocalDate inDate = toDate(in.get("bizDate"));
                int k = bucket(limits, ChronoUnit.DAYS.between(inDate, date));
                ageQty[k] = ageQty[k].add(take);
                remaining = remaining.subtract(take);
                oldest = inDate;
            }
            // 入库记录不足以覆盖结存（如期初没有入库流水）时计入最老一段
            if (remaining.signum() > 0) {
                ageQty[n - 1] = ageQty[n - 1].add(remaining);
            }
            // 各段金额 = 数量 × 单价，四舍五入的尾差计入最后一个有数量的段
            BigDecimal amount = money(qty.multiply(unit));
            BigDecimal[] ageAmount = new BigDecimal[n];
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < n; i++) {
                ageAmount[i] = money(ageQty[i].multiply(unit));
                allocated = allocated.add(ageAmount[i]);
            }
            int last = lastNonZero(ageQty);
            ageAmount[last] = ageAmount[last].add(amount.subtract(allocated));
            List<Map<String, Object>> ages = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ages.add(Map.of("qty", ageQty[i], "amount", ageAmount[i]));
                totalQty[i] = totalQty[i].add(ageQty[i]);
                totalAmount[i] = totalAmount[i].add(ageAmount[i]);
            }
            Map<String, Object> row = new LinkedHashMap<>(b);
            row.put("unitCost", unit);
            row.put("amount", amount);
            row.put("oldestDate", remaining.signum() > 0 ? null : oldest);
            row.put("ages", ages);
            rows.add(row);
            sumQty = sumQty.add(qty);
            sumAmount = sumAmount.add(amount);
        }
        List<Map<String, Object>> totalAges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            totalAges.add(Map.of("qty", totalQty[i], "amount", totalAmount[i]));
        }
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("qty", sumQty);
        total.put("amount", sumAmount);
        total.put("ages", totalAges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asOf", date);
        result.put("buckets", labels);
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    private static int bucket(List<Integer> limits, long days) {
        for (int i = 0; i < limits.size(); i++) {
            if (days <= limits.get(i)) {
                return i;
            }
        }
        return limits.size();
    }

    private static int lastNonZero(BigDecimal[] values) {
        for (int i = values.length - 1; i >= 0; i--) {
            if (values[i].signum() != 0) {
                return i;
            }
        }
        return values.length - 1;
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }
}
