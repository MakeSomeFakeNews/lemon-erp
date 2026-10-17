package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.bi.entity.BiEtlLog;
import com.lemon.erp.modules.bi.mapper.BiEtlLogMapper;
import com.lemon.erp.modules.bi.mapper.BiEtlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据仓库同步：从业务表全量重建销售、采购、生产、财务事实表，并保存当天的库存快照（历史快照保留，用于库存趋势）。
 * 由定时任务「数据仓库同步」（{@link BiEtlJob}，默认 lemon.bi.etl-cron）执行，也可以在「数据仓库」中手动执行；每个租户同一时间只运行一个同步任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BiEtlService {

    /** 人力事实表计算的月份数 */
    private static final int HR_MONTHS = 24;
    /** 每条 INSERT 写入的行数 */
    private static final int BATCH_SIZE = 500;

    /** 事实表及写入的列，列名与 BiEtlMapper 查询的列别名一致 */
    private record Fact(String table, List<String> columns) {
    }

    private static final Fact SALES = new Fact("bi_fact_sales", List.of("biz_date", "period", "doc_no", "customer_id",
            "owner_id", "material_id", "category", "warehouse_id", "dept_id", "qty", "amount", "cost", "profit"));
    private static final Fact PURCHASE = new Fact("bi_fact_purchase", List.of("biz_date", "period", "doc_no", "supplier_id",
            "material_id", "category", "warehouse_id", "qty", "amount"));
    private static final Fact INVENTORY = new Fact("bi_fact_inventory", List.of("snap_date", "period", "warehouse_id",
            "material_id", "category", "qty", "amount"));
    private static final Fact PRODUCTION = new Fact("bi_fact_production", List.of("wo_no", "period", "material_id", "category",
            "dept_id", "status", "plan_qty", "finished_qty", "scrap_qty", "hours", "late"));
    private static final Fact FINANCE = new Fact("bi_fact_finance", List.of("period", "account_id", "category", "dept_id",
            "debit", "credit"));
    private static final Fact HR = new Fact("bi_fact_hr", List.of("period", "dept_id", "category", "headcount", "hires",
            "leaves", "gross_pay", "labor_cost"));
    private static final Fact CRM = new Fact("bi_fact_crm", List.of("period", "owner_id", "dept_id", "leads", "followups",
            "opps", "won", "lost", "won_amount"));
    private static final Fact PROJECT = new Fact("bi_fact_project", List.of("period", "project_id", "dept_id", "manager_id",
            "hours", "labor_cost", "expense", "revenue", "cost"));

    private final BiEtlMapper etlMapper;
    private final BiEtlLogMapper logMapper;
    private final TransactionTemplate transactionTemplate;
    /** 正在同步的租户，同一租户同一时间只运行一个同步任务 */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    /**
     * 执行同步
     *
     * @param trigger MANUAL 手动 / SCHEDULE 定时
     * @return 同步日志
     */
    public BiEtlLog run(String trigger) {
        Long tenantId = TenantContext.require();
        if (!running.add(tenantId)) {
            throw new BizException("bi.error.dataSyncRunningTryAgainLater");
        }
        BiEtlLog entry = new BiEtlLog();
        entry.setTriggerType(trigger);
        entry.setStartTime(LocalDateTime.now());
        entry.setStatus("RUNNING");
        entry.setCreateBy("MANUAL".equals(trigger) ? SecurityUtils.getUserIdOrNull() : null);
        try {
            logMapper.insert(entry);
            Map<String, Integer> counts = transactionTemplate.execute(status -> load());
            entry.setStatus("SUCCESS");
            entry.setRowCounts(format(counts));
        } catch (RuntimeException e) {
            log.error("数据仓库同步失败", e);
            entry.setStatus("FAILED");
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            entry.setMessage(msg.length() > 1000 ? msg.substring(0, 1000) : msg);
        } finally {
            entry.setEndTime(LocalDateTime.now());
            if (entry.getId() != null) {
                logMapper.updateById(entry);
            }
            running.remove(tenantId);
        }
        if ("FAILED".equals(entry.getStatus())) {
            throw new BizException("bi.error.dataSyncFailed", entry.getMessage());
        }
        return entry;
    }

    public PageResult<BiEtlLog> logs(long current, long size, String status) {
        Page<BiEtlLog> page = Page.of(current, size);
        logMapper.selectPageList(page, status);
        return PageResult.of(page);
    }

    /** 各事实表行数、最近一次成功同步 */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>(etlMapper.countRows());
        m.put("running", running.contains(TenantContext.require()));
        m.put("lastSuccess", lastSuccess());
        return m;
    }

    /** 最近一次成功同步的时间，没有时为空 */
    public LocalDateTime lastSuccess() {
        List<BiEtlLog> list = logMapper.selectPage(Page.of(1, 1, false), Wrappers.<BiEtlLog>lambdaQuery()
                .eq(BiEtlLog::getStatus, "SUCCESS").orderByDesc(BiEtlLog::getId)).getRecords();
        return list.isEmpty() ? null : list.get(0).getEndTime();
    }

    private Map<String, Integer> load() {
        LocalDate today = LocalDate.now();
        Map<String, Integer> counts = new LinkedHashMap<>();
        etlMapper.clearSales();
        counts.put("sales", write(SALES, etlMapper.selectSales(), Map.of()));
        etlMapper.costSales();
        etlMapper.profitSales();
        etlMapper.clearPurchase();
        counts.put("purchase", write(PURCHASE, etlMapper.selectPurchase(), Map.of()));
        etlMapper.clearInventory(today);
        counts.put("inventory", write(INVENTORY, etlMapper.selectInventory(),
                Map.of("snap_date", today, "period", today.getYear() * 100 + today.getMonthValue(), "amount", 0)));
        etlMapper.costInventory(today);
        etlMapper.clearProduction();
        counts.put("production", write(PRODUCTION, etlMapper.selectProduction(today), Map.of()));
        etlMapper.clearFinance();
        counts.put("finance", write(FINANCE, etlMapper.selectFinance(), Map.of()));
        // 人力按月份逐月计算（月末在职人数），最近 24 个月
        etlMapper.clearHr();
        int hr = 0;
        YearMonth month = YearMonth.from(today);
        for (int i = 0; i < HR_MONTHS; i++, month = month.minusMonths(1)) {
            int period = month.getYear() * 100 + month.getMonthValue();
            hr += write(HR, etlMapper.selectHr(period, month.atDay(1), month.atEndOfMonth()), Map.of("period", period));
        }
        counts.put("hr", hr);
        etlMapper.clearCrm();
        counts.put("crm", write(CRM, etlMapper.selectCrm(), Map.of()));
        etlMapper.clearProject();
        counts.put("project", write(PROJECT, etlMapper.selectProject(), Map.of()));
        return counts;
    }

    /**
     * 把查询结果分批写入事实表
     *
     * @param fixed 所有行相同的列值（如快照日期），优先于查询结果
     * @return 写入行数
     */
    private int write(Fact fact, List<Map<String, Object>> rows, Map<String, Object> fixed) {
        List<List<Object>> values = rows.stream()
                .map(row -> fact.columns().stream().map(c -> fixed.containsKey(c) ? fixed.get(c) : row.get(c)).toList())
                .toList();
        for (int from = 0; from < values.size(); from += BATCH_SIZE) {
            etlMapper.insertFacts(fact.table(), fact.columns(), values.subList(from, Math.min(from + BATCH_SIZE, values.size())));
        }
        return values.size();
    }

    private static String format(Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        counts.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : ",").append(k).append('=').append(v));
        return sb.toString();
    }
}
