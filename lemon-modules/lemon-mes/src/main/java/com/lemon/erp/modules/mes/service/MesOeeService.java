package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.StopRequest;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesEquipmentStop;
import com.lemon.erp.modules.mes.mapper.MesCalendarMapper;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesEquipmentStopMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.StopReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备 OEE（综合效率）= 可用率 × 性能率 × 良品率：
 * <ul>
 *   <li>计划时间 = 工作日历工时（设备所属工作中心，每台设备按一个资源）- 计划停机（保养、无生产计划）</li>
 *   <li>可用率 = 运行时间 / 计划时间，运行时间 = 计划时间 - 非计划停机（故障、换型、待料等）</li>
 *   <li>性能率 = 理论工时 / 运行时间，理论工时 = Σ 报工（合格 + 报废）× 工序标准工时</li>
 *   <li>良品率 = 合格 / (合格 + 报废)</li>
 * </ul>
 * 停机按天切分，单日停机不超过当天的日历工时
 */
@Service
@RequiredArgsConstructor
public class MesOeeService {

    private static final int MAX_DAYS = 366;
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private final MesEquipmentMapper equipmentMapper;
    private final MesEquipmentStopMapper stopMapper;
    private final MesWorkCenterMapper workCenterMapper;
    private final MesCalendarMapper calendarMapper;

    /** 一台设备（某天或期间合计）的 OEE；比率为 0~1 的小数，分母为 0 时为空 */
    public record Oee(Long equipmentId, String equipmentCode, String equipmentName, String workCenterName, LocalDate date,
                      BigDecimal calendarHours, BigDecimal plannedStopHours, BigDecimal plannedHours, BigDecimal downtimeHours,
                      BigDecimal runHours, BigDecimal goodQty, BigDecimal scrapQty, BigDecimal idealHours,
                      BigDecimal availability, BigDecimal performance, BigDecimal quality, BigDecimal oee) {
    }

    /** 设备 OEE 明细：期间合计、每天、按原因的停机时长 */
    public record Detail(Oee total, List<Oee> days, Map<StopReason, BigDecimal> downtime) {
    }

    /** 期间内各设备（未报废）的 OEE */
    public List<Oee> summary(LocalDate begin, LocalDate end, Long workCenterId) {
        checkRange(begin, end);
        List<MesEquipment> equipments = equipmentMapper.selectList(Wrappers.<MesEquipment>lambdaQuery()
                .ne(MesEquipment::getStatus, EquipmentStatus.SCRAPPED)
                .eq(workCenterId != null, MesEquipment::getWorkCenterId, workCenterId)
                .orderByAsc(MesEquipment::getCode));
        Calc calc = new Calc(begin, end, null);
        return equipments.stream().map(e -> calc.total(e, calc.days(e))).toList();
    }

    public Detail detail(Long equipmentId, LocalDate begin, LocalDate end) {
        checkRange(begin, end);
        MesEquipment e = equipmentMapper.selectById(equipmentId);
        if (e == null) {
            throw new BizException("设备不存在");
        }
        Calc calc = new Calc(begin, end, equipmentId);
        List<Oee> days = calc.days(e);
        return new Detail(calc.total(e, days), days, calc.downtimeByReason(e.getId()));
    }

    // ---------------------------------------------------------------- 停机记录

    public PageResult<MesEquipmentStop> stops(long current, long size, Long equipmentId, LocalDate beginDate, LocalDate endDate,
                                              StopReason reason) {
        Page<MesEquipmentStop> page = Page.of(current, size);
        stopMapper.selectPageList(page, equipmentId, beginDate == null ? null : beginDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay(), reason);
        return PageResult.of(page);
    }

    @Transactional
    public Long createStop(StopRequest req) {
        MesEquipmentStop stop = new MesEquipmentStop();
        fill(stop, req, null);
        stop.setCreateBy(SecurityUtils.getUserId());
        stopMapper.insert(stop);
        return stop.getId();
    }

    @Transactional
    public void updateStop(Long id, StopRequest req) {
        MesEquipmentStop stop = manualStop(id);
        fill(stop, req, id);
        stopMapper.updateById(stop);
    }

    @Transactional
    public void deleteStop(Long id) {
        manualStop(id);
        stopMapper.deleteById(id);
    }

    private MesEquipmentStop manualStop(Long id) {
        MesEquipmentStop stop = id == null ? null : stopMapper.selectById(id);
        if (stop == null) {
            throw new BizException("停机记录不存在");
        }
        if (stop.getMaintenanceId() != null) {
            throw new BizException("维护单自动记录的停机不能修改或删除");
        }
        return stop;
    }

    private void fill(MesEquipmentStop stop, StopRequest req, Long excludeId) {
        MesEquipment e = equipmentMapper.selectById(req.equipmentId());
        if (e == null || e.getStatus() == EquipmentStatus.SCRAPPED) {
            throw new BizException("设备不存在或已报废");
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        if (Duration.between(req.startTime(), req.endTime()).toDays() > 31) {
            throw new BizException("一次停机不能超过 31 天，请分段登记");
        }
        boolean overlap = stopMapper.selectOverlapping(req.equipmentId(), req.startTime(), req.endTime()).stream()
                .anyMatch(s -> !s.getId().equals(excludeId));
        if (overlap) {
            throw new BizException("与该设备已有的停机记录时间重叠");
        }
        stop.setEquipmentId(req.equipmentId());
        stop.setStartTime(req.startTime());
        stop.setEndTime(req.endTime());
        stop.setHours(hours(req.startTime(), req.endTime()));
        stop.setReason(req.reason());
        stop.setRemark(req.remark());
    }

    private static void checkRange(LocalDate begin, LocalDate end) {
        if (end.isBefore(begin)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("统计范围不能超过 " + MAX_DAYS + " 天");
        }
    }

    private static BigDecimal hours(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Duration.between(from, to).toSeconds()).divide(SECONDS_PER_HOUR, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? null : a.divide(b, 4, RoundingMode.HALF_UP);
    }

    /** 一次计算：日历、停机、报工 */
    private final class Calc {

        private final LocalDate begin;
        private final LocalDate end;
        private final WorkCalendar calendar;
        /** 设备 → 日期 → [计划停机, 非计划停机] */
        private final Map<Long, Map<LocalDate, BigDecimal[]>> stops = new HashMap<>();
        /** 设备 → 原因 → 时长（期间内） */
        private final Map<Long, Map<StopReason, BigDecimal>> reasons = new HashMap<>();
        /** 设备 → 日期 → [合格, 报废, 理论工时] */
        private final Map<Long, Map<LocalDate, BigDecimal[]>> output = new HashMap<>();

        Calc(LocalDate begin, LocalDate end, Long equipmentId) {
            this.begin = begin;
            this.end = end;
            calendar = new WorkCalendar(workCenterMapper, calendarMapper, begin, end);
            for (MesEquipmentStop s : stopMapper.selectOverlapping(equipmentId, begin.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
                for (LocalDate d = max(s.getStartTime().toLocalDate(), begin); !d.isAfter(min(s.getEndTime().toLocalDate(), end)); d = d.plusDays(1)) {
                    LocalDateTime from = s.getStartTime().isAfter(d.atStartOfDay()) ? s.getStartTime() : d.atStartOfDay();
                    LocalDateTime to = s.getEndTime().isBefore(d.plusDays(1).atStartOfDay()) ? s.getEndTime() : d.plusDays(1).atStartOfDay();
                    if (!to.isAfter(from)) {
                        continue;
                    }
                    BigDecimal h = hours(from, to);
                    BigDecimal[] v = stops.computeIfAbsent(s.getEquipmentId(), k -> new HashMap<>())
                            .computeIfAbsent(d, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    int idx = s.getReason().isPlanned() ? 0 : 1;
                    v[idx] = v[idx].add(h);
                    reasons.computeIfAbsent(s.getEquipmentId(), k -> new EnumMap<>(StopReason.class)).merge(s.getReason(), h, BigDecimal::add);
                }
            }
            for (Map<String, Object> r : stopMapper.selectOutput(equipmentId, begin, end)) {
                LocalDate d = r.get("reportDate") instanceof java.sql.Date sd ? sd.toLocalDate() : LocalDate.parse(r.get("reportDate").toString());
                output.computeIfAbsent(((Number) r.get("equipmentId")).longValue(), k -> new HashMap<>())
                        .put(d, new BigDecimal[]{dec(r.get("goodQty")), dec(r.get("scrapQty")), dec(r.get("idealHours"))});
            }
        }

        List<Oee> days(MesEquipment e) {
            List<Oee> list = new ArrayList<>();
            Map<LocalDate, BigDecimal[]> st = stops.getOrDefault(e.getId(), Map.of());
            Map<LocalDate, BigDecimal[]> out = output.getOrDefault(e.getId(), Map.of());
            for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
                BigDecimal cal = calendar.perResource(e.getWorkCenterId(), d);
                BigDecimal[] s = st.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal plannedStop = s[0].min(cal);
                BigDecimal planned = cal.subtract(plannedStop);
                BigDecimal down = s[1].min(planned);
                BigDecimal[] o = out.getOrDefault(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                list.add(oee(e, d, cal, plannedStop, planned, down, o[0], o[1], o[2]));
            }
            return list;
        }

        Oee total(MesEquipment e, List<Oee> days) {
            BigDecimal cal = BigDecimal.ZERO;
            BigDecimal plannedStop = BigDecimal.ZERO;
            BigDecimal planned = BigDecimal.ZERO;
            BigDecimal down = BigDecimal.ZERO;
            BigDecimal good = BigDecimal.ZERO;
            BigDecimal scrap = BigDecimal.ZERO;
            BigDecimal ideal = BigDecimal.ZERO;
            for (Oee d : days) {
                cal = cal.add(d.calendarHours());
                plannedStop = plannedStop.add(d.plannedStopHours());
                planned = planned.add(d.plannedHours());
                down = down.add(d.downtimeHours());
                good = good.add(d.goodQty());
                scrap = scrap.add(d.scrapQty());
                ideal = ideal.add(d.idealHours());
            }
            return oee(e, null, cal, plannedStop, planned, down, good, scrap, ideal);
        }

        Map<StopReason, BigDecimal> downtimeByReason(Long equipmentId) {
            return reasons.getOrDefault(equipmentId, Map.of());
        }

        private Oee oee(MesEquipment e, LocalDate d, BigDecimal cal, BigDecimal plannedStop, BigDecimal planned, BigDecimal down,
                        BigDecimal good, BigDecimal scrap, BigDecimal ideal) {
            BigDecimal run = planned.subtract(down);
            BigDecimal a = ratio(run, planned);
            BigDecimal p = ratio(ideal, run);
            BigDecimal q = ratio(good, good.add(scrap));
            BigDecimal oee = a == null || p == null || q == null ? null : a.multiply(p).multiply(q).setScale(4, RoundingMode.HALF_UP);
            return new Oee(e.getId(), e.getCode(), e.getName(), e.getWorkCenterId() == null ? null : calendar.name(e.getWorkCenterId()), d,
                    cal, plannedStop, planned, down, run, good, scrap, ideal.setScale(2, RoundingMode.HALF_UP), a, p, q, oee);
        }
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
