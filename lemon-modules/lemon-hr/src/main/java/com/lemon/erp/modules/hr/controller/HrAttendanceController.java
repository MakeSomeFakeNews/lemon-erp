package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.dto.AttendanceRequest;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.entity.HrHoliday;
import com.lemon.erp.modules.hr.entity.HrShift;
import com.lemon.erp.modules.hr.service.HrAttendanceService;
import com.lemon.erp.modules.hr.service.HrCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 考勤记录 /hr/attendance、班次 /hr/shift、节假日 /hr/holiday
 */
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrAttendanceController {

    private final HrAttendanceService attendanceService;
    private final HrCalendarService calendarService;

    @GetMapping("/attendance")
    @PreAuthorize("@ss.hasPerm('hr:attendance:list')")
    public R<PageResult<HrAttendance>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) LocalDate beginDate,
                                            @RequestParam(required = false) LocalDate endDate) {
        return R.ok(attendanceService.page(current, size, keyword, deptId, status, beginDate, endDate));
    }

    /** 月度汇总，period：yyyyMM */
    @GetMapping("/attendance/summary")
    @PreAuthorize("@ss.hasPerm('hr:attendance:list')")
    public R<List<AttendanceSummary>> summary(@RequestParam int period,
                                              @RequestParam(required = false) Long deptId,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(attendanceService.summaryPage(period, deptId, keyword));
    }

    @PostMapping("/attendance")
    @PreAuthorize("@ss.hasPerm('hr:attendance:edit')")
    @OperLog(title = "考勤补录", type = OperType.UPDATE)
    public R<Void> manual(@Valid @RequestBody AttendanceRequest req) {
        attendanceService.manual(req);
        return R.ok();
    }

    @DeleteMapping("/attendance/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:attendance:edit')")
    @OperLog(title = "考勤记录", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        attendanceService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 班次、节假日

    @GetMapping("/shift")
    @PreAuthorize("@ss.hasAnyPerm('hr:attendance:list', 'hr:employee:list')")
    public R<List<HrShift>> shifts() {
        return R.ok(calendarService.shifts());
    }

    @PostMapping("/shift")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "班次设置", type = OperType.INSERT)
    public R<Void> createShift(@Valid @RequestBody HrShift shift) {
        calendarService.saveShift(null, shift);
        return R.ok();
    }

    @PutMapping("/shift/{id}")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "班次设置", type = OperType.UPDATE)
    public R<Void> updateShift(@PathVariable Long id, @Valid @RequestBody HrShift shift) {
        calendarService.saveShift(id, shift);
        return R.ok();
    }

    @DeleteMapping("/shift/{id}")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "班次设置", type = OperType.DELETE)
    public R<Void> deleteShift(@PathVariable Long id) {
        calendarService.deleteShift(id);
        return R.ok();
    }

    @GetMapping("/holiday")
    @PreAuthorize("@ss.hasPerm('hr:attendance:list')")
    public R<List<HrHoliday>> holidays(@RequestParam int year) {
        return R.ok(calendarService.holidays(year));
    }

    @PostMapping("/holiday")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "节假日设置", type = OperType.INSERT)
    public R<Void> createHoliday(@Valid @RequestBody HrHoliday holiday) {
        calendarService.saveHoliday(null, holiday);
        return R.ok();
    }

    @PutMapping("/holiday/{id}")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "节假日设置", type = OperType.UPDATE)
    public R<Void> updateHoliday(@PathVariable Long id, @Valid @RequestBody HrHoliday holiday) {
        calendarService.saveHoliday(id, holiday);
        return R.ok();
    }

    @DeleteMapping("/holiday/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:attendance:config')")
    @OperLog(title = "节假日设置", type = OperType.DELETE)
    public R<Void> deleteHoliday(@PathVariable List<Long> ids) {
        calendarService.deleteHoliday(ids);
        return R.ok();
    }
}
