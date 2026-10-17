package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.bi.dto.BiScreen;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 数据大屏：生产车间、生产进度、仓储物流、销售经营、质量管理。每个大屏一次返回全部数据，页面定时刷新；
 * 按当前租户统计全部数据（不按部门拆分），登录查看需要「全部数据」权限，投屏链接由有该权限的用户创建
 */
@Service
@RequiredArgsConstructor
public class BiScreenService {

    private static final int TREND_DAYS = 14;

    private final BiScreenMapper screenMapper;
    private final MesBoardService boardService;

    /**
     * @param workCenterId 生产车间、生产进度大屏只统计该工作中心，为空统计全部
     */
    public BiScreen.ScreenData data(ScreenKey key, Long workCenterId) {
        LocalDate today = LocalDate.now();
        String name = key.name();
        String title = Messages.get(key.getLabel());
        String time = LocalDateTime.now().withNano(0).toString();
        return switch (key) {
            case FACTORY -> factory(name, title, time, today, workCenterId);
            case PROGRESS -> progress(name, title, time, workCenterId);
            case WAREHOUSE -> warehouse(name, title, time, today);
            case SALES -> sales(name, title, time, today);
            case QUALITY -> quality(name, title, time, today);
        };
    }

    // ---------------------------------------------------------------- 生产车间

    private BiScreen.Factory factory(String key, String title, String time, LocalDate today, Long wc) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        BiScreen.ReportSum todayReport = screenMapper.reportSum(today, today, wc);
        BiScreen.ReportSum monthReport = screenMapper.reportSum(monthBegin, today, wc);
        BiScreen.WorkOrderCounts counts = screenMapper.workOrderCounts(today, today.plusDays(1), wc);
        List<BiScreen.Equipment> equipments = screenMapper.equipments(today, monthBegin, wc);
        long running = equipments.stream().filter(e -> e.getStatus() == EquipmentStatus.RUNNING).count();

        BiScreen.FactoryKpi kpi = new BiScreen.FactoryKpi(todayReport.getGood(), todayReport.getScrap(), yieldRate(todayReport),
                todayReport.getHours(), monthReport.getGood(), yieldRate(monthReport),
                wc == null ? screenMapper.finishedInQty(today) : null,
                counts.getActive(), counts.getOverdue(), counts.getCompleted(), equipments.size(), running,
                equipments.isEmpty() ? null : BigDecimal.valueOf(running * 100.0 / equipments.size()).setScale(1, RoundingMode.HALF_UP));

        List<BiScreen.ReportDay> trend = fillDays(screenMapper.reportTrend(today.minusDays(TREND_DAYS - 1), today, wc), today,
                BiScreen.ReportDay::getDay, d -> {
                    BiScreen.ReportDay r = new BiScreen.ReportDay();
                    r.setDay(d);
                    return r;
                }, r -> {
                    r.setGood(zero(r.getGood()));
                    r.setScrap(zero(r.getScrap()));
                });
        return new BiScreen.Factory(key, title, time, wc == null ? null : screenMapper.workCenterName(wc), kpi, trend,
                screenMapper.workCenterLoad(today, wc), equipments,
                screenMapper.stopReasons(monthBegin, today.plusDays(1), wc),
                screenMapper.activeWorkOrders(wc, 50), screenMapper.recentReports(wc, 30));
    }

    // ---------------------------------------------------------------- 生产进度

    /** 生产进度：与「生产看板」相同的数据 */
    private BiScreen.Progress progress(String key, String title, String time, Long wc) {
        BoardView board = boardService.board(wc);
        return new BiScreen.Progress(key, title, time, board.workCenter(), board.kpi(), board.lines(), board.orders(), board.hourly());
    }

    // ---------------------------------------------------------------- 仓储物流

    private BiScreen.Warehouse warehouse(String key, String title, String time, LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        // 当日已完成的出入库单：没有时单数为 0、数量为 0
        long inDocs = 0;
        long outDocs = 0;
        BigDecimal inQty = BigDecimal.ZERO;
        BigDecimal outQty = BigDecimal.ZERO;
        for (BiScreen.StockDocSum r : screenMapper.stockDocSum(today, today)) {
            if (r.getKind() == StockBizType.IN) {
                inDocs = num(r.getDocs());
                inQty = r.getQty();
            } else {
                outDocs = num(r.getDocs());
                outQty = r.getQty();
            }
        }
        BiScreen.DeliveryCounts deliveries = screenMapper.deliveryCounts(today);
        BiScreen.StockOverview overview = screenMapper.stockOverview();

        BiScreen.WarehouseKpi kpi = new BiScreen.WarehouseKpi(inDocs, inQty, outDocs, outQty,
                deliveries.getPending(), deliveries.getPicked(), deliveries.getShipped(), screenMapper.transitCount(),
                overview.getSkus(), overview.getQty(), screenMapper.lowStockCount(), screenMapper.expiringBatchCount(today.plusDays(30)));

        // 数据库按出入库分行，合并为每天一行
        Map<LocalDate, BiScreen.StockDay> byDay = new LinkedHashMap<>();
        for (BiScreen.StockDocDay r : screenMapper.stockDocTrend(today.minusDays(TREND_DAYS - 1), today)) {
            BiScreen.StockDay day = byDay.computeIfAbsent(r.getDay(), d -> {
                BiScreen.StockDay x = new BiScreen.StockDay();
                x.setDay(d);
                return x;
            });
            if (r.getQty() != null) {
                if (r.getKind() == StockBizType.IN) {
                    day.setInQty(r.getQty());
                } else {
                    day.setOutQty(r.getQty());
                }
            }
        }
        List<BiScreen.StockDay> trend = fillDays(new ArrayList<>(byDay.values()), today, BiScreen.StockDay::getDay, d -> {
            BiScreen.StockDay x = new BiScreen.StockDay();
            x.setDay(d);
            return x;
        }, r -> {
            r.setInQty(zero(r.getInQty()));
            r.setOutQty(zero(r.getOutQty()));
        });

        List<BiScreen.RecentStockDoc> docs = screenMapper.recentStockDocs(30);
        for (BiScreen.RecentStockDoc r : docs) {
            if (r.getKind() == StockBizType.TRANSFER) {
                r.setTypeLabel(Messages.get("bi.text.transfer"));
            }
        }
        return new BiScreen.Warehouse(key, title, time, kpi, trend, screenMapper.stockDocByType(monthBegin, today),
                screenMapper.warehouseStock(), screenMapper.lowStock(30), screenMapper.expiringBatches(today.plusDays(30), 30),
                screenMapper.pendingDeliveries(30), docs);
    }

    // ---------------------------------------------------------------- 销售经营

    private BiScreen.Sales sales(String key, String title, String time, LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        LocalDate yearBegin = today.withDayOfYear(1);
        BiScreen.OrderSum todayOrders = screenMapper.salesOrderSum(today, today);
        BiScreen.OrderSum monthOrders = screenMapper.salesOrderSum(monthBegin, today);
        BiScreen.OrderSum yearOrders = screenMapper.salesOrderSum(yearBegin, today);

        BiScreen.SalesKpi kpi = new BiScreen.SalesKpi(todayOrders.getAmount(), todayOrders.getOrders(), monthOrders.getAmount(),
                yearOrders.getAmount(), screenMapper.shippedAmount(today, today), screenMapper.shippedAmount(monthBegin, today),
                screenMapper.shippedAmount(yearBegin, today), screenMapper.shippedAmount(yearBegin.minusYears(1), today.minusYears(1)),
                screenMapper.receiptAmount(monthBegin, today), screenMapper.receiptAmount(yearBegin, today),
                screenMapper.receivableBalance(), screenMapper.overdueOrderCount(today), screenMapper.newCustomerCount(monthBegin));

        Map<Integer, BigDecimal> orders = toPeriodMap(screenMapper.monthlyOrders(yearBegin, today));
        Map<Integer, BigDecimal> shipped = toPeriodMap(screenMapper.monthlyShipped(yearBegin, today));
        Map<Integer, BigDecimal> lastShipped = toPeriodMap(screenMapper.monthlyShipped(yearBegin.minusYears(1), yearBegin.minusYears(1).withMonth(12).withDayOfMonth(31)));
        List<BiScreen.SalesMonth> monthly = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int period = today.getYear() * 100 + m;
            boolean past = m <= today.getMonthValue();
            monthly.add(new BiScreen.SalesMonth(m, past ? orders.getOrDefault(period, BigDecimal.ZERO) : null,
                    past ? shipped.getOrDefault(period, BigDecimal.ZERO) : null, lastShipped.getOrDefault(period - 100, BigDecimal.ZERO)));
        }

        List<BiScreen.NameAmount> salesmen = screenMapper.salesmen(monthBegin, today, 8);
        salesmen.stream().filter(r -> r.getName() == null).forEach(r -> r.setName(Messages.get("bi.text.unassigned")));
        return new BiScreen.Sales(key, title, time, kpi, monthly, screenMapper.topCustomers(yearBegin, today, 10),
                screenMapper.topProducts(yearBegin, today, 10), salesmen, screenMapper.openOrderStatus(), screenMapper.recentOrders(30));
    }

    // ---------------------------------------------------------------- 质量管理

    private BiScreen.Quality quality(String key, String title, String time, LocalDate today) {
        LocalDate monthBegin = today.withDayOfMonth(1);
        List<BiScreen.InspectionType> byType = screenMapper.inspectionByType(monthBegin, today);
        long total = 0;
        long pass = 0;
        long iqcTotal = 0;
        long iqcPass = 0;
        for (BiScreen.InspectionType r : byType) {
            long t = num(r.getTotal());
            long p = num(r.getPass());
            total += t;
            pass += p;
            if (r.getType() == QcType.IQC) {
                iqcTotal += t;
                iqcPass += p;
            }
            r.setRate(rate(p, t));
        }
        BiScreen.ReportSum report = screenMapper.reportSum(monthBegin, today, null);
        BiScreen.NcrCounts ncr = screenMapper.ncrCounts(monthBegin, today.plusDays(1));

        BiScreen.QualityKpi kpi = new BiScreen.QualityKpi(total, rate(pass, total), rate(iqcPass, iqcTotal),
                rate(pass - iqcPass, total - iqcTotal), yieldRate(report), report.getScrap(),
                ncr.getOpen(), ncr.getCreated(), ncr.getDone());

        List<BiScreen.InspectionDay> trend = fillDays(screenMapper.inspectionTrend(today.minusDays(TREND_DAYS - 1), today), today,
                BiScreen.InspectionDay::getDay, d -> {
                    BiScreen.InspectionDay r = new BiScreen.InspectionDay();
                    r.setDay(d);
                    return r;
                }, r -> {
                    r.setTotal(num(r.getTotal()));
                    r.setPass(num(r.getPass()));
                    r.setRate(rate(r.getPass(), r.getTotal()));
                });
        List<BiScreen.SupplierQuality> suppliers = screenMapper.supplierQuality(monthBegin, today, 8);
        for (BiScreen.SupplierQuality r : suppliers) {
            long t = num(r.getTotal());
            r.setRate(rate(t - num(r.getFail()), t));
        }
        return new BiScreen.Quality(key, title, time, kpi, byType, trend, screenMapper.defectTypes(monthBegin, today, 8), suppliers,
                screenMapper.openNcrs(30), screenMapper.recentInspections(30));
    }

    // ---------------------------------------------------------------- 工具

    /** 最近 14 天每天一行，没有数据的日期新建一行；每行再用 fill 把空值补为 0 */
    private static <T> List<T> fillDays(List<T> rows, LocalDate today, Function<T, LocalDate> dayOf, Function<LocalDate, T> create,
                                        Consumer<T> fill) {
        Map<LocalDate, T> byDay = new HashMap<>();
        for (T r : rows) {
            byDay.put(dayOf.apply(r), r);
        }
        List<T> result = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            T row = byDay.computeIfAbsent(today.minusDays(i), create);
            fill.accept(row);
            result.add(row);
        }
        return result;
    }

    private static Map<Integer, BigDecimal> toPeriodMap(List<BiScreen.PeriodAmount> rows) {
        Map<Integer, BigDecimal> m = new HashMap<>();
        for (BiScreen.PeriodAmount r : rows) {
            m.put(r.getPeriod(), r.getAmount());
        }
        return m;
    }

    /** 良品率 = 合格 ÷（合格 + 报废），没有报工时为空 */
    private static BigDecimal yieldRate(BiScreen.ReportSum report) {
        BigDecimal good = zero(report.getGood());
        BigDecimal all = good.add(zero(report.getScrap()));
        return Decimals.percent(good, all, 1);
    }

    private static BigDecimal rate(long part, long total) {
        return total == 0 ? null : BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private static long num(Long v) {
        return v == null ? 0 : v;
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
