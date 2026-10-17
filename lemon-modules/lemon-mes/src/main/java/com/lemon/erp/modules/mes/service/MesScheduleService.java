package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.mes.dto.CapacityLoad;
import com.lemon.erp.modules.mes.entity.MesCalendar;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesCalendarMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 产能与排程。工作中心日产能 = 当天每个资源的工时（工厂日历 → 工作中心工作日 × 每日工时）× 资源数。
 * <ul>
 *   <li>单个工单排程：从计划开工日（早于今天按今天）起按工序顺序向后排，每道工序的剩余工时 = 未报工数量 × 标准工时，
 *       逐日占用工作中心的产能（不考虑其他工单，无限产能）</li>
 *   <li>全部排程：未结束的工单按计划开工日、工单号依次排，共享工作中心的产能（有限产能），先排的优先</li>
 *   <li>负荷：各工序剩余工时平均分摊到排程区间的工作日，与产能对比</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MesScheduleService {

    private static final int MAX_DAYS = 730;

    private final MesWorkOrderMapper workOrderMapper;
    private final MesWoOpMapper woOpMapper;
    private final MesWorkCenterMapper workCenterMapper;
    private final MesCalendarMapper calendarMapper;
    private final MesWorkOrderService workOrderService;

    /** 单个工单排程（无限产能），返回排程后的工单 */
    @Transactional
    public MesWorkOrder scheduleOne(Long woId) {
        MesWorkOrder wo = workOrderService.getAccessible(woId);
        if (!wo.getStatus().isOpen()) {
            throw new BizException("mes.error.onlyUnfinishedWorkOrdersScheduled");
        }
        LocalDate today = LocalDate.now();
        schedule(wo, calendar(today), new HashMap<>(), today);
        return workOrderMapper.selectById(woId);
    }

    /** 全部未结束的工单按优先顺序排程（有限产能），返回排程的工单数 */
    @Transactional
    public int scheduleAll() {
        LocalDate today = LocalDate.now();
        WorkCalendar capacity = calendar(today);
        Map<String, BigDecimal> used = new HashMap<>();
        List<MesWorkOrder> list = workOrderMapper.selectList(Wrappers.<MesWorkOrder>lambdaQuery().in(MesWorkOrder::getStatus, WorkOrderStatus.OPEN));
        list.sort(Comparator.comparing((MesWorkOrder w) -> w.getPlanStart() == null ? LocalDate.MAX : w.getPlanStart())
                .thenComparing(MesWorkOrder::getId));
        for (MesWorkOrder wo : list) {
            schedule(wo, capacity, used, today);
        }
        return list.size();
    }

    private void schedule(MesWorkOrder wo, WorkCalendar capacity, Map<String, BigDecimal> used, LocalDate today) {
        List<MesWoOp> ops = woOpMapper.selectByWo(wo.getId());
        if (ops.isEmpty()) {
            return;
        }
        ops.sort(Comparator.comparing(MesWoOp::getSeq));
        LocalDate cursor = wo.getPlanStart() == null || wo.getPlanStart().isBefore(today) ? today : wo.getPlanStart();
        LocalDate first = null;
        for (MesWoOp op : ops) {
            BigDecimal need = remainingHours(wo, op);
            LocalDate start = null;
            LocalDate d = cursor;
            int guard = 0;
            while (need.signum() > 0) {
                if (++guard > MAX_DAYS) {
                    throw new BizException("mes.error.workCenterLacksCapacityWithinDays", capacity.name(op.getWorkCenterId()), MAX_DAYS);
                }
                String key = op.getWorkCenterId() + "|" + d;
                BigDecimal free = capacity.hours(op.getWorkCenterId(), d).subtract(used.getOrDefault(key, BigDecimal.ZERO));
                if (free.signum() > 0) {
                    BigDecimal take = free.min(need);
                    used.merge(key, take, BigDecimal::add);
                    need = need.subtract(take);
                    if (start == null) {
                        start = d;
                    }
                    if (need.signum() <= 0) {
                        break;
                    }
                }
                d = d.plusDays(1);
            }
            if (start == null) {
                start = cursor;
                d = cursor;
            }
            woOpMapper.update(Wrappers.<MesWoOp>lambdaUpdate().set(MesWoOp::getPlanStart, start).set(MesWoOp::getPlanEnd, d)
                    .eq(MesWoOp::getId, op.getId()));
            first = first == null ? start : first;
            cursor = d;
        }
        workOrderMapper.update(Wrappers.<MesWorkOrder>lambdaUpdate().set(MesWorkOrder::getPlanStart, first)
                .set(MesWorkOrder::getPlanEnd, cursor).eq(MesWorkOrder::getId, wo.getId()));
    }

    /**
     * 工作中心负荷：[{workCenterId, code, name, capacity, load, days: [{date, capacity, load}], ops: [...]}]
     */
    public List<CapacityLoad> load(LocalDate from, LocalDate to, Long workCenterId) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 92) {
            throw new BizException("mes.error.queryRangeCannotExceedDays");
        }
        LocalDate today = LocalDate.now();
        WorkCalendar capacity = calendar(from.isBefore(today) ? from : today);
        Map<Long, MesWorkOrder> wos = workOrderMapper.selectList(Wrappers.<MesWorkOrder>lambdaQuery().in(MesWorkOrder::getStatus, WorkOrderStatus.OPEN))
                .stream().collect(Collectors.toMap(MesWorkOrder::getId, Function.identity()));
        Map<Long, Map<LocalDate, BigDecimal>> loads = new HashMap<>();
        Map<Long, List<CapacityLoad.Op>> opRows = new HashMap<>();
        if (!wos.isEmpty()) {
            for (MesWoOp op : woOpMapper.selectList(Wrappers.<MesWoOp>lambdaQuery().in(MesWoOp::getWoId, wos.keySet())
                    .eq(workCenterId != null, MesWoOp::getWorkCenterId, workCenterId))) {
                MesWorkOrder wo = wos.get(op.getWoId());
                BigDecimal hours = remainingHours(wo, op);
                if (hours.signum() <= 0) {
                    continue;
                }
                LocalDate s = op.getPlanStart() != null ? op.getPlanStart() : wo.getPlanStart() != null ? wo.getPlanStart() : today;
                LocalDate e = op.getPlanEnd() != null ? op.getPlanEnd() : wo.getPlanEnd() != null ? wo.getPlanEnd() : s;
                if (e.isBefore(s)) {
                    e = s;
                }
                if (e.isBefore(from) || s.isAfter(to)) {
                    continue;
                }
                List<LocalDate> days = new ArrayList<>();
                for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                    if (capacity.hours(op.getWorkCenterId(), d).signum() > 0) {
                        days.add(d);
                    }
                }
                if (days.isEmpty()) {
                    days.add(s);
                }
                BigDecimal perDay = hours.divide(BigDecimal.valueOf(days.size()), 4, RoundingMode.HALF_UP);
                Map<LocalDate, BigDecimal> wcLoad = loads.computeIfAbsent(op.getWorkCenterId(), k -> new HashMap<>());
                days.forEach(d -> wcLoad.merge(d, perDay, BigDecimal::add));
                opRows.computeIfAbsent(op.getWorkCenterId(), k -> new ArrayList<>()).add(new CapacityLoad.Op(wo.getId(), wo.getWoNo(),
                        wo.getStatus(), op.getId(), op.getSeq(), op.getName(), s, e, hours));
            }
        }
        List<CapacityLoad> result = new ArrayList<>();
        for (MesWorkCenter wc : capacity.centers()) {
            if (workCenterId != null && !workCenterId.equals(wc.getId()) || !Integer.valueOf(1).equals(wc.getStatus())) {
                continue;
            }
            List<CapacityLoad.Day> days = new ArrayList<>();
            BigDecimal capTotal = BigDecimal.ZERO;
            BigDecimal loadTotal = BigDecimal.ZERO;
            Map<LocalDate, BigDecimal> wcLoad = loads.getOrDefault(wc.getId(), Map.of());
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                BigDecimal cap = capacity.hours(wc.getId(), d);
                BigDecimal l = wcLoad.getOrDefault(d, BigDecimal.ZERO);
                capTotal = capTotal.add(cap);
                loadTotal = loadTotal.add(l);
                days.add(new CapacityLoad.Day(d, cap, l.setScale(2, RoundingMode.HALF_UP)));
            }
            loadTotal = loadTotal.setScale(2, RoundingMode.HALF_UP);
            List<CapacityLoad.Op> ops = new ArrayList<>(opRows.getOrDefault(wc.getId(), List.of()));
            ops.sort(Comparator.comparing(CapacityLoad.Op::planStart));
            result.add(new CapacityLoad(wc.getId(), wc.getCode(), wc.getName(), capTotal, loadTotal, days, ops));
        }
        return result;
    }

    // ---------------------------------------------------------------- 工厂日历

    public List<MesCalendar> calendar(LocalDate from, LocalDate to, Long workCenterId) {
        return calendarMapper.selectList(Wrappers.<MesCalendar>lambdaQuery().ge(MesCalendar::getCalDate, from).le(MesCalendar::getCalDate, to)
                .eq(workCenterId != null, MesCalendar::getWorkCenterId, workCenterId).orderByAsc(MesCalendar::getCalDate));
    }

    /** 设置某天的可用工时（同一工作中心同一天覆盖） */
    @Transactional
    public void saveCalendar(MesCalendar c) {
        if (c.getCalDate() == null || c.getHours() == null || c.getHours().signum() < 0 || c.getHours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new BizException("mes.error.enterDateHoursBetween");
        }
        Long wc = c.getWorkCenterId() == null ? 0L : c.getWorkCenterId();
        if (wc != 0 && workCenterMapper.selectById(wc) == null) {
            throw new BizException("mes.error.workCenterDoesNotExist");
        }
        calendarMapper.delete(Wrappers.<MesCalendar>lambdaQuery().eq(MesCalendar::getWorkCenterId, wc).eq(MesCalendar::getCalDate, c.getCalDate()));
        c.setId(null);
        c.setWorkCenterId(wc);
        calendarMapper.insert(c);
    }

    public void deleteCalendar(Long id) {
        calendarMapper.deleteById(id);
    }

    private WorkCalendar calendar(LocalDate from) {
        return new WorkCalendar(workCenterMapper, calendarMapper, from, from.plusDays(MAX_DAYS + 92));
    }

    private static BigDecimal remainingHours(MesWorkOrder wo, MesWoOp op) {
        BigDecimal left = wo.getQty().subtract(op.getGoodQty()).subtract(op.getScrapQty());
        return left.signum() <= 0 ? BigDecimal.ZERO : left.multiply(op.getStdHours());
    }
}
