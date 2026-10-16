package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.dto.CalendarDay;
import com.lemon.erp.modules.hr.dto.SelfProfile;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrShift;
import com.lemon.erp.modules.hr.model.HrConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 员工自助：当前登录账号关联的员工查看本人档案、考勤、假期余额、培训、工资条、考核
 */
@Service
@RequiredArgsConstructor
public class HrSelfService {

    private final HrEmployeeService employeeService;
    private final HrAttendanceService attendanceService;
    private final HrLeaveService leaveService;
    private final HrTrainingService trainingService;
    private final HrPerfService perfService;
    private final HrCalendarService calendarService;

    /** 没有关联员工档案时 employee 为空 */
    public SelfProfile profile() {
        HrEmployee current = employeeService.currentOrNull();
        Long userId = SecurityUtils.getUserId();
        if (current == null) {
            return new SelfProfile(null, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, perfService.pendingCounts(userId, null)[1], BigDecimal.ZERO);
        }
        HrEmployee e = employeeService.detail(current.getId());
        int year = LocalDate.now().getYear();
        HrShift shift = calendarService.shiftOf(e);
        int annual = HrConstants.annualLeaveDays(e.getWorkStartDate() != null ? e.getWorkStartDate() : e.getEntryDate(), LocalDate.of(year, 12, 31));
        BigDecimal used = leaveService.annualUsedHours(e.getId(), year).divide(shift.getWorkHours(), 1, RoundingMode.HALF_UP);
        long[] pending = perfService.pendingCounts(userId, e.getId());
        return new SelfProfile(e, annual, used, trainingService.yearHours(e.getId(), year), pending[0], pending[1],
                leaveService.compensatoryBalance(e.getId()));
    }

    public List<CalendarDay> calendar(int period) {
        return attendanceService.calendar(employeeService.current(), HrAttendanceService.toYearMonth(period));
    }

    public AttendanceSummary summary(int period) {
        HrEmployee e = employeeService.current();
        HrEmployee detail = employeeService.detail(e.getId());
        return attendanceService.summarize(List.of(detail), HrAttendanceService.toYearMonth(period)).get(0);
    }
}
