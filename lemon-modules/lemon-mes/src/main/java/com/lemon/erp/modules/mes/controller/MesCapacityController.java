package com.lemon.erp.modules.mes.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.mes.entity.MesCalendar;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.service.MesScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 产能与排程：工作中心负荷、工单排程、工厂日历
 */
@RestController
@RequestMapping("/mes/capacity")
@RequiredArgsConstructor
public class MesCapacityController {

    private final MesScheduleService scheduleService;

    @GetMapping("/load")
    @PreAuthorize("@ss.hasPerm('mes:capacity:list')")
    public R<List<Map<String, Object>>> load(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) Long workCenterId) {
        return R.ok(scheduleService.load(from, to, workCenterId));
    }

    @PostMapping("/schedule/{woId}")
    @PreAuthorize("@ss.hasAnyPerm('mes:capacity:edit', 'mes:wo:edit')")
    @OperLog(title = "工单排程", type = OperType.UPDATE)
    public R<MesWorkOrder> scheduleOne(@PathVariable Long woId) {
        return R.ok(scheduleService.scheduleOne(woId));
    }

    /** 全部未结束的工单按有限产能重新排程，返回工单数 */
    @PostMapping("/schedule-all")
    @PreAuthorize("@ss.hasPerm('mes:capacity:edit')")
    @OperLog(title = "全部排程", type = OperType.UPDATE)
    public R<Integer> scheduleAll() {
        return R.ok(scheduleService.scheduleAll());
    }

    @GetMapping("/calendar")
    @PreAuthorize("@ss.hasPerm('mes:capacity:list')")
    public R<List<MesCalendar>> calendar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) Long workCenterId) {
        return R.ok(scheduleService.calendar(from, to, workCenterId));
    }

    @PostMapping("/calendar")
    @PreAuthorize("@ss.hasPerm('mes:capacity:edit')")
    @OperLog(title = "工厂日历", type = OperType.UPDATE)
    public R<Void> saveCalendar(@RequestBody MesCalendar calendar) {
        scheduleService.saveCalendar(calendar);
        return R.ok();
    }

    @DeleteMapping("/calendar/{id}")
    @PreAuthorize("@ss.hasPerm('mes:capacity:edit')")
    @OperLog(title = "工厂日历", type = OperType.DELETE)
    public R<Void> deleteCalendar(@PathVariable Long id) {
        scheduleService.deleteCalendar(id);
        return R.ok();
    }
}
