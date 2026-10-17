package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.bi.mapper.BiScreenMapper;
import com.lemon.erp.modules.bi.model.ScreenKey;
import com.lemon.erp.modules.mes.dto.BoardView;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.service.MesBoardService;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏：生产车间、生产进度、仓储物流、销售经营、质量管理。每个大屏一次返回全部数据，页面定时刷新；
 * 按当前租户统计全部数据（不按部门拆分），登录查看需要「全部数据」权限，投屏链接由有该权限的用户创建
 */
@Service
@RequiredArgsConstructor
public class BiScreenService {

    private static final int TREND_DAYS = 14;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BiScreenMapper screenMapper;
    private final MesBoardService boardService;

    /**
     * @param workCenterId 生产车间、生产进度大屏只统计该工作中心，为空统计全部
     */
    public Map<String, Object> data(ScreenKey key, Long workCenterId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> data = switch (key) {
            case FACTORY -> factory(today, workCenterId);
            case PROGRESS -> progress(workCenterId);
            case WAREHOUSE -> warehouse(today);
            case SALES -> sales(today);
            case QUALITY -> quality(today);
        };
        data.put("key", key.name());
        data.put("title", Messages.get(key.getLabel()));
        data.put("time", LocalDateTime.now().withNano(0).toString());
        return data;
    }

    // ---------------------------------------------------------------- 生产车间

    private Map<String, Object> factory(LocalDate today, Long wc) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        Map<String, Object> todayReport = screenMapper.reportSum(today, today, wc);
        Map<String, Object> monthReport = screenMapper.reportSum(monthBegin, today, wc);
        Map<String, Object> counts = screenMapper.workOrderCounts(today, today.plusDays(1), wc);
        List<Map<String, Object>> equipments = screenMapper.equipments(today, monthBegin, wc);
        long running = equipments.stream().filter(e -> EquipmentStatus.RUNNING.name().equals(e.get("status"))).count();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayGood", todayReport.get("good"));
        kpi.put("todayScrap", todayReport.get("scrap"));
        kpi.put("todayYield", yieldRate(todayReport));
        kpi.put("todayHours", todayReport.get("hours"));
        kpi.put("monthGood", monthReport.get("good"));
        kpi.put("monthYield", yieldRate(monthReport));
        kpi.put("todayFinishedIn", wc == null ? screenMapper.finishedInQty(today) : null);
        kpi.put("activeOrders", counts.get("active"));
        kpi.put("overdueOrders", counts.get("overdue"));
        kpi.put("completedOrders", counts.get("completed"));
        kpi.put("equipments", equipments.size());
        kpi.put("runningEquipments", running);
        kpi.put("runningRate", equipments.isEmpty() ? null
                : BigDecimal.valueOf(running * 100.0 / equipments.size()).setScale(1, RoundingMode.HALF_UP));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workCenter", wc == null ? null : screenMapper.workCenterName(wc));
        data.put("kpi", kpi);
        data.put("trend", fillDays(screenMapper.reportTrend(today.minusDays(TREND_DAYS - 1), today, wc), today, "good", "scrap"));
        data.put("workCenters", screenMapper.workCenterLoad(today, wc));
        data.put("equipments", equipments);
        data.put("stopReasons", screenMapper.stopReasons(monthBegin, today.plusDays(1), wc));
        data.put("workOrders", screenMapper.activeWorkOrders(wc, 50));
        data.put("reports", screenMapper.recentReports(wc, 30));
        return data;
    }

    // ---------------------------------------------------------------- 生产进度

    /** 生产进度：与「生产看板」相同的数据 */
    private Map<String, Object> progress(Long wc) {
        BoardView board = boardService.board(wc);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workCenter", board.workCenter());
        data.put("kpi", board.kpi());
        data.put("lines", board.lines());
        data.put("orders", board.orders());
        data.put("hourly", board.hourly());
        return data;
    }

    // ---------------------------------------------------------------- 仓储物流

    private Map<String, Object> warehouse(LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        Map<String, Object> todayDocs = byKind(screenMapper.stockDocSum(today, today));
        Map<String, Object> deliveries = screenMapper.deliveryCounts(today);
        Map<String, Object> overview = screenMapper.stockOverview();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayInDocs", todayDocs.get("inDocs"));
        kpi.put("todayInQty", todayDocs.get("inQty"));
        kpi.put("todayOutDocs", todayDocs.get("outDocs"));
        kpi.put("todayOutQty", todayDocs.get("outQty"));
        kpi.put("pendingPick", deliveries.get("pending"));
        kpi.put("pendingShip", deliveries.get("picked"));
        kpi.put("shippedToday", deliveries.get("shipped"));
        kpi.put("transit", screenMapper.transitCount());
        kpi.put("skus", overview.get("skus"));
        kpi.put("stockQty", overview.get("qty"));
        kpi.put("lowStock", screenMapper.lowStockCount());
        kpi.put("expiring", screenMapper.expiringBatchCount(today.plusDays(30)));

        List<Map<String, Object>> trendRows = screenMapper.stockDocTrend(today.minusDays(TREND_DAYS - 1), today);
        List<Map<String, Object>> pivot = new ArrayList<>();
        for (Map<String, Object> r : trendRows) {
            Map<String, Object> row = new HashMap<>();
            row.put("day", r.get("day"));
            row.put(StockBizType.IN.name().equals(r.get("kind")) ? "inQty" : "outQty", r.get("qty"));
            pivot.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kpi", kpi);
        data.put("trend", fillDays(pivot, today, "inQty", "outQty"));
        data.put("byType", screenMapper.stockDocByType(monthBegin, today));
        data.put("warehouses", screenMapper.warehouseStock());
        data.put("lowStock", screenMapper.lowStock(30));
        data.put("expiring", screenMapper.expiringBatches(today.plusDays(30), 30));
        data.put("deliveries", screenMapper.pendingDeliveries(30));
        List<Map<String, Object>> docs = screenMapper.recentStockDocs(30);
        for (Map<String, Object> r : docs) {
            if (StockBizType.TRANSFER.name().equals(r.get("kind"))) {
                r.put("typeLabel", Messages.get("bi.text.transfer"));
            }
        }
        data.put("docs", docs);
        return data;
    }

    // ---------------------------------------------------------------- 销售经营

    private Map<String, Object> sales(LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        LocalDate yearBegin = today.withDayOfYear(1);
        Map<String, Object> todayOrders = screenMapper.salesOrderSum(today, today);
        Map<String, Object> monthOrders = screenMapper.salesOrderSum(monthBegin, today);
        Map<String, Object> yearOrders = screenMapper.salesOrderSum(yearBegin, today);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayOrderAmount", todayOrders.get("amount"));
        kpi.put("todayOrders", todayOrders.get("orders"));
        kpi.put("monthOrderAmount", monthOrders.get("amount"));
        kpi.put("yearOrderAmount", yearOrders.get("amount"));
        kpi.put("todayShipped", screenMapper.shippedAmount(today, today));
        kpi.put("monthShipped", screenMapper.shippedAmount(monthBegin, today));
        kpi.put("yearShipped", screenMapper.shippedAmount(yearBegin, today));
        BigDecimal lastYear = screenMapper.shippedAmount(yearBegin.minusYears(1), today.minusYears(1));
        kpi.put("lastYearShipped", lastYear);
        kpi.put("monthReceipt", screenMapper.receiptAmount(monthBegin, today));
        kpi.put("yearReceipt", screenMapper.receiptAmount(yearBegin, today));
        kpi.put("receivable", screenMapper.receivableBalance());
        kpi.put("overdueOrders", screenMapper.overdueOrderCount(today));
        kpi.put("newCustomers", screenMapper.newCustomerCount(monthBegin));

        Map<Integer, Object> orders = toPeriodMap(screenMapper.monthlyOrders(yearBegin, today));
        Map<Integer, Object> shipped = toPeriodMap(screenMapper.monthlyShipped(yearBegin, today));
        Map<Integer, Object> lastShipped = toPeriodMap(screenMapper.monthlyShipped(yearBegin.minusYears(1), yearBegin.minusYears(1).withMonth(12).withDayOfMonth(31)));
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int period = today.getYear() * 100 + m;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", m);
            row.put("orders", m <= today.getMonthValue() ? orders.getOrDefault(period, BigDecimal.ZERO) : null);
            row.put("shipped", m <= today.getMonthValue() ? shipped.getOrDefault(period, BigDecimal.ZERO) : null);
            row.put("lastYear", lastShipped.getOrDefault(period - 100, BigDecimal.ZERO));
            monthly.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kpi", kpi);
        data.put("monthly", monthly);
        data.put("topCustomers", screenMapper.topCustomers(yearBegin, today, 10));
        data.put("topProducts", screenMapper.topProducts(yearBegin, today, 10));
        List<Map<String, Object>> salesmen = screenMapper.salesmen(monthBegin, today, 8);
        salesmen.forEach(r -> r.putIfAbsent("name", Messages.get("bi.text.unassigned")));
        data.put("salesmen", salesmen);
        data.put("orderStatus", screenMapper.openOrderStatus());
        data.put("orders", screenMapper.recentOrders(30));
        return data;
    }

    // ---------------------------------------------------------------- 质量管理

    private Map<String, Object> quality(LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        List<Map<String, Object>> byType = screenMapper.inspectionByType(monthBegin, today);
        long total = 0;
        long pass = 0;
        long iqcTotal = 0;
        long iqcPass = 0;
        for (Map<String, Object> r : byType) {
            long t = num(r.get("total"));
            long p = num(r.get("pass"));
            total += t;
            pass += p;
            if (QcType.IQC.name().equals(r.get("type"))) {
                iqcTotal += t;
                iqcPass += p;
            }
            r.put("rate", rate(p, t));
        }
        Map<String, Object> report = screenMapper.reportSum(monthBegin, today, null);
        Map<String, Object> ncr = screenMapper.ncrCounts(monthBegin, today.plusDays(1));

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("inspections", total);
        kpi.put("passRate", rate(pass, total));
        kpi.put("iqcRate", rate(iqcPass, iqcTotal));
        kpi.put("processRate", rate(pass - iqcPass, total - iqcTotal));
        kpi.put("productionYield", yieldRate(report));
        kpi.put("scrapQty", report.get("scrap"));
        kpi.put("openNcr", ncr.get("open"));
        kpi.put("monthNcr", ncr.get("created"));
        kpi.put("doneNcr", ncr.get("done"));

        List<Map<String, Object>> trend = fillDays(screenMapper.inspectionTrend(today.minusDays(TREND_DAYS - 1), today), today, "total", "pass");
        for (Map<String, Object> r : trend) {
            r.put("rate", rate(num(r.get("pass")), num(r.get("total"))));
        }
        List<Map<String, Object>> suppliers = screenMapper.supplierQuality(monthBegin, today, 8);
        for (Map<String, Object> r : suppliers) {
            long t = num(r.get("total"));
            r.put("rate", rate(t - num(r.get("fail")), t));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kpi", kpi);
        data.put("byType", byType);
        data.put("trend", trend);
        data.put("defects", screenMapper.defectTypes(monthBegin, today, 8));
        data.put("suppliers", suppliers);
        data.put("ncrs", screenMapper.openNcrs(30));
        data.put("inspections", screenMapper.recentInspections(30));
        return data;
    }

    // ---------------------------------------------------------------- 工具

    /** 补齐最近 14 天中没有数据的日期，fields 缺省为 0 */
    private static List<Map<String, Object>> fillDays(List<Map<String, Object>> rows, LocalDate today, String... fields) {
        Map<String, Map<String, Object>> byDay = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> merged = byDay.computeIfAbsent(String.valueOf(r.get("day")), k -> new LinkedHashMap<>());
            r.forEach((k, v) -> {
                if (v != null) {
                    merged.put(k, v);
                }
            });
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> row = new LinkedHashMap<>(byDay.getOrDefault(d.toString(), Map.of()));
            row.put("day", d.toString());
            for (String f : fields) {
                row.putIfAbsent(f, BigDecimal.ZERO);
            }
            result.add(row);
        }
        return result;
    }

    private static Map<String, Object> byKind(List<Map<String, Object>> rows) {
        Map<String, Object> m = new HashMap<>(Map.of("inDocs", 0, "inQty", BigDecimal.ZERO, "outDocs", 0, "outQty", BigDecimal.ZERO));
        for (Map<String, Object> r : rows) {
            String prefix = StockBizType.IN.name().equals(r.get("kind")) ? "in" : "out";
            m.put(prefix + "Docs", r.get("docs"));
            m.put(prefix + "Qty", r.get("qty"));
        }
        return m;
    }

    private static Map<Integer, Object> toPeriodMap(List<Map<String, Object>> rows) {
        Map<Integer, Object> m = new HashMap<>();
        for (Map<String, Object> r : rows) {
            m.put(((Number) r.get("period")).intValue(), r.get("amount"));
        }
        return m;
    }

    /** 良品率 = 合格 ÷（合格 + 报废），没有报工时为空 */
    private static BigDecimal yieldRate(Map<String, Object> report) {
        BigDecimal good = dec(report.get("good"));
        BigDecimal all = good.add(dec(report.get("scrap")));
        return all.signum() == 0 ? null : good.multiply(HUNDRED).divide(all, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(long part, long total) {
        return total == 0 ? null : BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private static long num(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
