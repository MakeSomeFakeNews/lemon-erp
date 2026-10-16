package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.mes.entity.MesCalendar;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.mapper.MesCalendarMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作日历：工作中心某天每个资源的可用工时。优先级：工作中心的日历设置 > 全厂日历设置 > 工作中心的工作日 × 每日工时。
 * 没有工作中心时按全厂日历，没有设置的周一至周五 8 小时
 */
final class WorkCalendar {

    private static final BigDecimal DEFAULT_HOURS = BigDecimal.valueOf(8);

    private final Map<Long, MesWorkCenter> centers;
    private final Map<String, BigDecimal> overrides = new HashMap<>();
    private final Map<Long, Set<Integer>> workDays = new HashMap<>();

    WorkCalendar(MesWorkCenterMapper workCenterMapper, MesCalendarMapper calendarMapper, LocalDate from, LocalDate to) {
        centers = workCenterMapper.selectList(Wrappers.<MesWorkCenter>lambdaQuery().orderByAsc(MesWorkCenter::getCode)).stream()
                .collect(Collectors.toMap(MesWorkCenter::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        calendarMapper.selectList(Wrappers.<MesCalendar>lambdaQuery().ge(MesCalendar::getCalDate, from).le(MesCalendar::getCalDate, to))
                .forEach(c -> overrides.put(c.getWorkCenterId() + "|" + c.getCalDate(), c.getHours()));
        centers.values().forEach(wc -> workDays.put(wc.getId(), Arrays.stream((wc.getWorkDays() == null ? "" : wc.getWorkDays()).split(","))
                .filter(x -> !x.isBlank()).map(x -> Integer.valueOf(x.trim())).collect(Collectors.toSet())));
    }

    Collection<MesWorkCenter> centers() {
        return centers.values();
    }

    String name(Long wcId) {
        MesWorkCenter wc = centers.get(wcId);
        return wc == null ? String.valueOf(wcId) : wc.getName();
    }

    /** 每个资源（一台设备 / 一个班组）当天的可用工时 */
    BigDecimal perResource(Long wcId, LocalDate d) {
        MesWorkCenter wc = wcId == null ? null : centers.get(wcId);
        BigDecimal hours = wc == null ? null : overrides.get(wcId + "|" + d);
        if (hours == null) {
            hours = overrides.get("0|" + d);
        }
        if (hours != null) {
            return hours;
        }
        if (wc == null) {
            return d.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue() ? DEFAULT_HOURS : BigDecimal.ZERO;
        }
        return workDays.get(wcId).contains(d.getDayOfWeek().getValue()) ? wc.getDailyHours() : BigDecimal.ZERO;
    }

    /** 工作中心当天的产能 = 每个资源的工时 × 资源数；工作中心不存在时为 0 */
    BigDecimal hours(Long wcId, LocalDate d) {
        MesWorkCenter wc = centers.get(wcId);
        if (wc == null) {
            return BigDecimal.ZERO;
        }
        return perResource(wcId, d).multiply(BigDecimal.valueOf(wc.getResources() == null ? 1 : wc.getResources()));
    }
}
