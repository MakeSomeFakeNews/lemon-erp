package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.mapper.FinStockAgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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
     */
    public FinAnalysis.StockAge analyze(LocalDate asOf, Long warehouseId, String category, String keyword, List<Integer> buckets) {
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        List<Integer> limits = buckets == null || buckets.isEmpty() ? DEFAULT_BUCKETS : buckets;
        for (int i = 0; i < limits.size(); i++) {
            if (limits.get(i) <= 0 || (i > 0 && limits.get(i) <= limits.get(i - 1))) {
                throw new BizException("fin.error.stockAgeBucketsMustIncreasingPositive");
            }
        }
        if (limits.size() > MAX_BUCKETS) {
            throw new BizException("fin.error.mostStockAgeBucketsAllowed", MAX_BUCKETS);
        }
        List<FinAnalysis.AgeBucket> labels = new ArrayList<>();
        for (int i = 0; i <= limits.size(); i++) {
            String label = i == 0 ? Messages.get("fin.text.days", 0, limits.get(0))
                    : i == limits.size() ? Messages.get("fin.text.overDays", limits.get(i - 1)) : Messages.get("fin.text.days", limits.get(i - 1) + 1, limits.get(i));
            labels.add(new FinAnalysis.AgeBucket(label));
        }
        List<FinAnalysis.StockAgeRow> balances = ageMapper.selectBalances(date, warehouseId, category, keyword);
        List<Long> ids = balances.stream().map(FinAnalysis.StockAgeRow::getMaterialId).toList();
        Map<Long, List<FinAnalysis.StockInflow>> inflows = new HashMap<>();
        Map<Long, BigDecimal> units = new HashMap<>();
        for (int i = 0; i < ids.size(); i += CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + CHUNK, ids.size()));
            ageMapper.selectInflows(date, warehouseId, chunk).forEach(r ->
                    inflows.computeIfAbsent(r.getMaterialId(), k -> new ArrayList<>()).add(r));
            ageMapper.selectUnitCosts(periodOf(date), chunk).forEach(r -> units.put(r.getMaterialId(), dec(r.getUnitCost())));
        }
        int n = limits.size() + 1;
        BigDecimal[] totalQty = zeros(n);
        BigDecimal[] totalAmount = zeros(n);
        BigDecimal sumQty = BigDecimal.ZERO;
        BigDecimal sumAmount = BigDecimal.ZERO;
        for (FinAnalysis.StockAgeRow b : balances) {
            Long id = b.getMaterialId();
            BigDecimal qty = dec(b.getQty());
            BigDecimal unit = units.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal[] ageQty = zeros(n);
            BigDecimal remaining = qty;
            LocalDate oldest = null;
            for (FinAnalysis.StockInflow in : inflows.getOrDefault(id, List.of())) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal take = remaining.min(dec(in.getQty()));
                LocalDate inDate = in.getBizDate();
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
            BigDecimal amount = Decimals.money(qty.multiply(unit));
            BigDecimal[] ageAmount = new BigDecimal[n];
            BigDecimal allocated = BigDecimal.ZERO;
            for (int i = 0; i < n; i++) {
                ageAmount[i] = Decimals.money(ageQty[i].multiply(unit));
                allocated = allocated.add(ageAmount[i]);
            }
            int last = lastNonZero(ageQty);
            ageAmount[last] = ageAmount[last].add(amount.subtract(allocated));
            List<FinAnalysis.AgeCell> ages = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ages.add(new FinAnalysis.AgeCell(ageQty[i], ageAmount[i]));
                totalQty[i] = totalQty[i].add(ageQty[i]);
                totalAmount[i] = totalAmount[i].add(ageAmount[i]);
            }
            b.setUnitCost(unit);
            b.setAmount(amount);
            b.setOldestDate(remaining.signum() > 0 ? null : oldest);
            b.setAges(ages);
            sumQty = sumQty.add(qty);
            sumAmount = sumAmount.add(amount);
        }
        List<FinAnalysis.AgeCell> totalAges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            totalAges.add(new FinAnalysis.AgeCell(totalQty[i], totalAmount[i]));
        }
        return new FinAnalysis.StockAge(date, labels, balances, new FinAnalysis.StockAgeTotal(sumQty, sumAmount, totalAges));
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

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
