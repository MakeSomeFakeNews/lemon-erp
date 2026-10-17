package com.lemon.erp.modules.mes.service;

import com.lemon.erp.modules.mes.dto.BoardOrderRow;
import com.lemon.erp.modules.mes.dto.BoardReportSum;
import com.lemon.erp.modules.mes.dto.BoardView;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.mapper.MesBoardMapper;
import com.lemon.erp.modules.mes.mapper.MesCalendarMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;
import com.lemon.erp.modules.mes.model.BoardLineState;
import com.lemon.erp.modules.mes.model.BoardOpState;
import com.lemon.erp.modules.mes.model.BoardOrderState;
import com.lemon.erp.modules.mes.model.DeliveryRisk;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生产看板：在制工单走到了哪道工序、还剩几道、预计哪天完工，各产线今天的生产情况。数据全部来自工单工序和报工，只读不写库。
 * <p>
 * 预计完工按有限产能推演（与「产能排程」的全部排程相同的规则，但不回写计划日期）：已下达、生产中的工单按计划开工日、工单号依次排，
 * 每道工序的剩余工时 = 未完成数量 × 单件工时（标准工时，没有时按已报工的实际工时折算），从今天起逐日占用工作中心的可用工时，
 * 今天已报工的工时先从产能中扣除；工序按顺序进行，后一道不早于前一道完成的日期
 */
@Service
@RequiredArgsConstructor
public class MesBoardService {

    /** 推演的最长天数，超过时视为产能不足、无法估算 */
    private static final int MAX_DAYS = 366;
    private static final int MAX_JOBS = 8;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MesBoardMapper boardMapper;
    private final MesWorkCenterMapper workCenterMapper;
    private final MesCalendarMapper calendarMapper;

    /**
     * @param workCenterId 只看某个工作中心（产线）：工单只列出有工序在这条产线上的，产线和今天的产出也只统计这条产线；
     *                     推演仍按全部工单（产能是共享的）
     */
    public BoardView board(Long workCenterId) {
        LocalDate today = LocalDate.now();
        WorkCalendar calendar = new WorkCalendar(workCenterMapper, calendarMapper, today, today.plusDays(MAX_DAYS));
        List<BoardOrderRow> rows = boardMapper.selectOrders();
        Map<Long, List<MesWoOp>> opsByWo = boardMapper.selectOps().stream()
                .collect(Collectors.groupingBy(MesWoOp::getWoId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, BoardReportSum> todayByOp = boardMapper.sumByOp(today).stream()
                .collect(Collectors.toMap(BoardReportSum::getId, Function.identity()));
        Map<Long, BoardReportSum> todayByWc = boardMapper.sumByWorkCenter(today).stream().filter(s -> s.getId() != null)
                .collect(Collectors.toMap(BoardReportSum::getId, Function.identity()));

        Map<Long, LocalDate> opEta = forecast(rows, opsByWo, todayByWc, calendar, today);

        List<BoardView.Order> orders = new ArrayList<>();
        for (BoardOrderRow row : rows) {
            List<MesWoOp> ops = opsByWo.getOrDefault(row.getId(), List.of());
            if (workCenterId == null || ops.stream().anyMatch(op -> workCenterId.equals(op.getWorkCenterId()))) {
                orders.add(order(row, ops, todayByOp, opEta, today));
            }
        }
        orders.sort(Comparator.comparing((BoardView.Order o) -> riskOrder(o.risk()))
                .thenComparing(BoardView.Order::eta, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BoardView.Order::id));

        List<BoardView.Line> lines = lines(calendar, rows, opsByWo, todayByOp, todayByWc, opEta, workCenterId, today);
        BoardView.Kpi kpi = kpi(orders, lines, todayByWc, workCenterId, today);
        return new BoardView(workCenterId == null ? null : calendar.name(workCenterId), kpi, lines, orders,
                boardMapper.sumByHour(today, workCenterId), LocalDateTime.now().withNano(0));
    }

    // ---------------------------------------------------------------- 推演

    /** 有限产能推演每道未完成工序的预计完成日期；产能不足（推演超过 MAX_DAYS）的工序及其后续工序没有日期 */
    private static Map<Long, LocalDate> forecast(List<BoardOrderRow> rows, Map<Long, List<MesWoOp>> opsByWo,
                                                 Map<Long, BoardReportSum> todayByWc, WorkCalendar calendar, LocalDate today) {
        Map<String, BigDecimal> used = new HashMap<>();
        todayByWc.forEach((wc, s) -> used.put(wc + "|" + today, nz(s.getHours())));
        List<BoardOrderRow> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((BoardOrderRow w) -> w.getPlanStart() == null ? LocalDate.MAX : w.getPlanStart())
                .thenComparing(BoardOrderRow::getId));
        Map<Long, LocalDate> eta = new HashMap<>();
        for (BoardOrderRow wo : sorted) {
            boolean started = wo.getStatus() == WorkOrderStatus.STARTED;
            LocalDate cursor = started || wo.getPlanStart() == null || wo.getPlanStart().isBefore(today) ? today : wo.getPlanStart();
            for (MesWoOp op : opsByWo.getOrDefault(wo.getId(), List.of())) {
                if (left(wo.getQty(), op).signum() <= 0) {
                    continue;
                }
                cursor = occupy(op.getWorkCenterId(), remainingHours(wo.getQty(), op), cursor, calendar, used, today);
                if (cursor == null) {
                    break;
                }
                eta.put(op.getId(), cursor);
            }
        }
        return eta;
    }

    /** 从 from 起逐日占用工作中心的产能，返回完成的日期；超过 MAX_DAYS 返回空 */
    private static LocalDate occupy(Long wcId, BigDecimal need, LocalDate from, WorkCalendar calendar,
                                    Map<String, BigDecimal> used, LocalDate today) {
        LocalDate d = from;
        while (need.signum() > 0) {
            if (d.isAfter(today.plusDays(MAX_DAYS))) {
                return null;
            }
            String key = wcId + "|" + d;
            BigDecimal free = calendar.hours(wcId, d).subtract(used.getOrDefault(key, BigDecimal.ZERO));
            if (free.signum() > 0) {
                BigDecimal take = free.min(need);
                used.merge(key, take, BigDecimal::add);
                need = need.subtract(take);
                if (need.signum() <= 0) {
                    break;
                }
            }
            d = d.plusDays(1);
        }
        return d;
    }

    // ---------------------------------------------------------------- 工单

    private static BoardView.Order order(BoardOrderRow row, List<MesWoOp> ops, Map<Long, BoardReportSum> todayByOp,
                                         Map<Long, LocalDate> opEta, LocalDate today) {
        BigDecimal qty = row.getQty();
        List<BoardView.Op> opViews = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;
        BigDecimal todayGood = BigDecimal.ZERO;
        int done = 0;
        boolean anyProgress = false;
        boolean unknown = false;
        LocalDate eta = null;
        MesWoOp current = null;
        for (MesWoOp op : ops) {
            BigDecimal opLeft = left(qty, op);
            BigDecimal opRemaining = remainingHours(qty, op);
            BigDecimal opTodayGood = todayByOp.containsKey(op.getId()) ? nz(todayByOp.get(op.getId()).getGood()) : BigDecimal.ZERO;
            BoardOpState state;
            if (opLeft.signum() <= 0) {
                state = BoardOpState.DONE;
                done++;
            } else {
                state = op.getGoodQty().add(op.getScrapQty()).signum() > 0 || opTodayGood.signum() > 0 ? BoardOpState.DOING : BoardOpState.WAITING;
                if (current == null) {
                    current = op;
                }
                LocalDate d = opEta.get(op.getId());
                if (d == null) {
                    unknown = true;
                } else if (eta == null || d.isAfter(eta)) {
                    eta = d;
                }
            }
            anyProgress |= op.getGoodQty().add(op.getScrapQty()).signum() > 0;
            total = total.add(qty.multiply(perUnit(op)));
            remaining = remaining.add(opRemaining);
            todayGood = todayGood.add(opTodayGood);
            opViews.add(new BoardView.Op(op.getId(), op.getSeq(), op.getName(), op.getWorkCenterName(), state,
                    op.getGoodQty(), op.getScrapQty(), opTodayGood, scale(opRemaining), opEta.get(op.getId())));
        }

        BigDecimal progress;
        if (ops.isEmpty()) {
            progress = rate(nz(row.getFinishedQty()), qty);
        } else if (total.signum() > 0) {
            progress = rate(total.subtract(remaining), total);
        } else {
            progress = rate(BigDecimal.valueOf(done), BigDecimal.valueOf(ops.size()));
        }
        boolean allDone = current == null;
        if (allDone) {
            // 工序已全部完成（或没有工序）：剩下完工入库，按今天计
            eta = today;
        } else if (unknown) {
            eta = null;
        }

        BoardOrderState state;
        if (allDone) {
            state = BoardOrderState.TO_STOCK;
        } else if (todayGood.signum() > 0 || row.getLastReportTime() != null && !row.getLastReportTime().toLocalDate().isBefore(today)) {
            state = BoardOrderState.RUNNING;
        } else if (anyProgress) {
            state = BoardOrderState.IDLE;
        } else {
            state = BoardOrderState.WAITING;
        }

        LocalDate due = row.getDueDate() != null ? row.getDueDate() : row.getPlanEnd();
        DeliveryRisk risk;
        if (due != null && due.isBefore(today)) {
            risk = DeliveryRisk.OVERDUE;
        } else if (eta == null) {
            risk = DeliveryRisk.UNKNOWN;
        } else if (due != null && eta.isAfter(due)) {
            risk = DeliveryRisk.AT_RISK;
        } else {
            risk = DeliveryRisk.ON_TIME;
        }
        return new BoardView.Order(row.getId(), row.getWoNo(), row.getWoType(), row.getStatus(), state, risk,
                row.getMaterialName(), row.getMaterialCode(), row.getSpec(), row.getUnit(), qty, row.getFinishedQty(),
                row.getSalesOrderNo(), row.getCustomerName(), row.getDueDate(), row.getPlanEnd(), eta, scale(remaining), progress,
                ops.size(), done, current == null ? null : current.getName(), current == null ? null : current.getWorkCenterName(),
                todayGood, row.getLastReportTime(), opViews);
    }

    private static int riskOrder(DeliveryRisk risk) {
        return switch (risk) {
            case OVERDUE -> 0;
            case AT_RISK -> 1;
            case UNKNOWN -> 2;
            case ON_TIME -> 3;
        };
    }

    // ---------------------------------------------------------------- 产线

    private static List<BoardView.Line> lines(WorkCalendar calendar, List<BoardOrderRow> rows, Map<Long, List<MesWoOp>> opsByWo,
                                              Map<Long, BoardReportSum> todayByOp, Map<Long, BoardReportSum> todayByWc,
                                              Map<Long, LocalDate> opEta, Long workCenterId, LocalDate today) {
        Map<Long, List<BoardView.Job>> jobs = new HashMap<>();
        Map<Long, BigDecimal> remaining = new HashMap<>();
        Map<Long, LocalDate> clear = new HashMap<>();
        Map<Long, Integer> queue = new HashMap<>();
        for (BoardOrderRow wo : rows) {
            Map<Long, Boolean> counted = new HashMap<>();
            for (MesWoOp op : opsByWo.getOrDefault(wo.getId(), List.of())) {
                BoardReportSum t = todayByOp.get(op.getId());
                boolean running = t != null && nz(t.getGood()).add(nz(t.getScrap())).add(nz(t.getHours())).signum() > 0;
                if (left(wo.getQty(), op).signum() <= 0 && !running) {
                    continue;
                }
                Long wc = op.getWorkCenterId();
                remaining.merge(wc, remainingHours(wo.getQty(), op), BigDecimal::add);
                LocalDate eta = opEta.get(op.getId());
                if (eta != null && (clear.get(wc) == null || eta.isAfter(clear.get(wc)))) {
                    clear.put(wc, eta);
                }
                if (left(wo.getQty(), op).signum() > 0 && counted.putIfAbsent(wc, true) == null) {
                    queue.merge(wc, 1, Integer::sum);
                }
                jobs.computeIfAbsent(wc, k -> new ArrayList<>()).add(new BoardView.Job(wo.getWoNo(), wo.getMaterialName(), op.getSeq(),
                        op.getName(), t == null ? BigDecimal.ZERO : nz(t.getGood()), running, eta));
            }
        }
        List<BoardView.Line> lines = new ArrayList<>();
        for (MesWorkCenter wc : calendar.centers()) {
            Long id = wc.getId();
            boolean enabled = Integer.valueOf(1).equals(wc.getStatus());
            if (workCenterId != null ? !workCenterId.equals(id) : !enabled && !queue.containsKey(id) && !todayByWc.containsKey(id)) {
                continue;
            }
            BoardReportSum t = todayByWc.get(id);
            BigDecimal capacity = calendar.hours(id, today);
            BigDecimal hours = t == null ? BigDecimal.ZERO : nz(t.getHours());
            BigDecimal good = t == null ? BigDecimal.ZERO : nz(t.getGood());
            BigDecimal scrap = t == null ? BigDecimal.ZERO : nz(t.getScrap());
            int q = queue.getOrDefault(id, 0);
            BoardLineState state;
            if (good.add(scrap).add(hours).signum() > 0) {
                state = BoardLineState.RUNNING;
            } else if (capacity.signum() <= 0) {
                state = BoardLineState.REST;
            } else if (q > 0) {
                state = BoardLineState.IDLE;
            } else {
                state = BoardLineState.FREE;
            }
            List<BoardView.Job> list = new ArrayList<>(jobs.getOrDefault(id, List.of()));
            list.sort(Comparator.comparing((BoardView.Job j) -> !j.running())
                    .thenComparing(BoardView.Job::eta, Comparator.nullsLast(Comparator.naturalOrder())));
            lines.add(new BoardView.Line(id, wc.getCode(), wc.getName(), state, capacity, hours, good, scrap,
                    capacity.signum() > 0 ? hours.multiply(HUNDRED).divide(capacity, 1, RoundingMode.HALF_UP) : null, q, scale(remaining.getOrDefault(id, BigDecimal.ZERO)),
                    clear.get(id), t == null ? null : t.getLastTime(), list.size() > MAX_JOBS ? list.subList(0, MAX_JOBS) : list));
        }
        return lines;
    }

    // ---------------------------------------------------------------- 汇总

    private BoardView.Kpi kpi(List<BoardView.Order> orders, List<BoardView.Line> lines, Map<Long, BoardReportSum> todayByWc,
                              Long workCenterId, LocalDate today) {
        Map<BoardOrderState, Long> states = orders.stream().collect(Collectors.groupingBy(BoardView.Order::state, Collectors.counting()));
        Map<DeliveryRisk, Long> risks = orders.stream().collect(Collectors.groupingBy(BoardView.Order::risk, Collectors.counting()));
        BigDecimal good = BigDecimal.ZERO;
        BigDecimal scrap = BigDecimal.ZERO;
        BigDecimal hours = BigDecimal.ZERO;
        for (Map.Entry<Long, BoardReportSum> e : todayByWc.entrySet()) {
            if (workCenterId == null || workCenterId.equals(e.getKey())) {
                good = good.add(nz(e.getValue().getGood()));
                scrap = scrap.add(nz(e.getValue().getScrap()));
                hours = hours.add(nz(e.getValue().getHours()));
            }
        }
        BigDecimal remaining = workCenterId == null
                ? orders.stream().map(BoardView.Order::remainingHours).reduce(BigDecimal.ZERO, BigDecimal::add)
                : lines.stream().map(BoardView.Line::remainingHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate allDone = risks.containsKey(DeliveryRisk.UNKNOWN) ? null : orders.stream().map(BoardView.Order::eta)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        return new BoardView.Kpi(orders.size(), count(states, BoardOrderState.RUNNING), count(states, BoardOrderState.IDLE),
                count(states, BoardOrderState.WAITING), count(states, BoardOrderState.TO_STOCK),
                count(risks, DeliveryRisk.AT_RISK), count(risks, DeliveryRisk.OVERDUE),
                boardMapper.countFinished(today, today.plusDays(1), workCenterId),
                good, scrap, good.add(scrap).signum() > 0 ? rate(good, good.add(scrap)) : null, scale(hours),
                (int) lines.stream().filter(l -> l.state() == BoardLineState.RUNNING).count(), lines.size(), scale(remaining), allDone);
    }

    // ---------------------------------------------------------------- 工具

    /** 工序未完成的数量 = 工单数量 - 合格 - 报废 */
    private static BigDecimal left(BigDecimal qty, MesWoOp op) {
        return qty.subtract(op.getGoodQty()).subtract(op.getScrapQty());
    }

    /** 单件工时：标准工时，没有设置时按已报工的实际工时折算 */
    private static BigDecimal perUnit(MesWoOp op) {
        if (op.getStdHours() != null && op.getStdHours().signum() > 0) {
            return op.getStdHours();
        }
        BigDecimal reported = op.getGoodQty().add(op.getScrapQty());
        if (reported.signum() > 0 && op.getHours() != null && op.getHours().signum() > 0) {
            return op.getHours().divide(reported, 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal remainingHours(BigDecimal qty, MesWoOp op) {
        BigDecimal left = left(qty, op);
        return left.signum() <= 0 ? BigDecimal.ZERO : left.multiply(perUnit(op));
    }

    private static BigDecimal rate(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(HUNDRED);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static <K> int count(Map<K, Long> map, K key) {
        return map.getOrDefault(key, 0L).intValue();
    }
}
