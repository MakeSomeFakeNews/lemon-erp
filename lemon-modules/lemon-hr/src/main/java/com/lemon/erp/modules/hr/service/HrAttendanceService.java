package com.lemon.erp.modules.hr.service;

import com.lemon.erp.modules.hr.model.AttendanceSource;
import com.lemon.erp.modules.hr.model.AttendanceStatus;
import com.lemon.erp.modules.hr.model.EmployeeStatus;
import com.lemon.erp.modules.hr.model.LeaveKind;
import com.lemon.erp.modules.hr.model.LeaveStatus;
import com.lemon.erp.modules.hr.model.OvertimeComp;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.hr.dto.AttendanceRequest;
import com.lemon.erp.modules.hr.dto.AttendanceSummary;
import com.lemon.erp.modules.hr.dto.CalendarDay;
import com.lemon.erp.modules.hr.dto.DayWork;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrHoliday;
import com.lemon.erp.modules.hr.entity.HrLeave;
import com.lemon.erp.modules.hr.entity.HrShift;
import com.lemon.erp.modules.hr.mapper.HrAttendanceMapper;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrLeaveMapper;
import com.lemon.erp.modules.hr.model.HrRules;
import com.lemon.erp.modules.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤：员工自助打卡（当天第一次为上班卡，之后为下班卡，取最后一次），人事补录；
 * 按班次计算迟到、早退、缺卡，按月汇总出勤、旷工、请假、加班，工资计算使用月度汇总
 */
@Service
@RequiredArgsConstructor
public class HrAttendanceService {

    /** 出勤超过 5 小时扣除 1 小时午休 */
    private static final long LUNCH_THRESHOLD_MINUTES = 300;

    private final HrAttendanceMapper attendanceMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrLeaveMapper leaveMapper;
    private final HrCalendarService calendarService;
    private final HrEmployeeService employeeService;
    private final SysDeptService deptService;
    private final HrPayrollLock payrollLock;
    private final HrRuleService ruleService;

    @DataPermission(table = "hr_employee", deptColumn = "dept_id", userColumn = "user_id")
    public PageResult<HrAttendance> page(long current, long size, String keyword, Long deptId, String status,
                                         LocalDate beginDate, LocalDate endDate) {
        Page<HrAttendance> page = Page.of(current, size);
        attendanceMapper.selectPageList(page, keyword, deptId == null ? null : deptService.selfAndDescendantIds(deptId),
                status, beginDate, endDate);
        return PageResult.of(page);
    }

    /** 当前员工打卡 */
    @Transactional
    public HrAttendance clock() {
        HrEmployee e = employeeService.current();
        if (e.getStatus() == EmployeeStatus.LEFT) {
            throw new BizException("hr.error.employeesWhoHaveLeftCannotClock");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDate today = now.toLocalDate();
        HrAttendance a = find(e.getId(), today);
        if (a == null) {
            a = new HrAttendance();
            a.setEmployeeId(e.getId());
            a.setWorkDate(today);
            a.setCheckIn(now);
            a.setSource(AttendanceSource.CLOCK);
            evaluate(a, calendarService.shiftOf(e), calendarService.isWorkday(today));
            try {
                attendanceMapper.insert(a);
                return a;
            } catch (DuplicateKeyException ex) {
                // 并发重复点击：已有当天记录，按下班卡处理
                a = find(e.getId(), today);
            }
        }
        a.setCheckOut(now);
        evaluate(a, calendarService.shiftOf(e), calendarService.isWorkday(today));
        saveTimes(a);
        return a;
    }

    /** 人事补录（补卡），覆盖当天打卡时间 */
    @Transactional
    public void manual(AttendanceRequest req) {
        HrEmployee e = employeeService.get(req.employeeId());
        if (req.checkIn() == null && req.checkOut() == null) {
            throw new BizException("hr.error.enterLeastClockClockOutTime");
        }
        for (LocalDateTime t : new LocalDateTime[]{req.checkIn(), req.checkOut()}) {
            if (t != null && !t.toLocalDate().equals(req.workDate())) {
                throw new BizException("hr.error.clockTimesMust", req.workDate());
            }
        }
        if (req.checkIn() != null && req.checkOut() != null && !req.checkOut().isAfter(req.checkIn())) {
            throw new BizException("hr.error.clockOutTimeMustLaterClock");
        }
        if (req.workDate().isBefore(e.getEntryDate()) || req.workDate().isAfter(LocalDate.now())) {
            throw new BizException("hr.error.attendanceOnlyRecordedDatesAfterHiring");
        }
        payrollLock.checkOpen(req.workDate(), "hr.label.addAttendanceRecords");
        HrAttendance a = find(e.getId(), req.workDate());
        boolean insert = a == null;
        if (insert) {
            a = new HrAttendance();
            a.setEmployeeId(e.getId());
            a.setWorkDate(req.workDate());
        }
        a.setCheckIn(req.checkIn());
        a.setCheckOut(req.checkOut());
        a.setSource(AttendanceSource.MANUAL);
        a.setRemark(req.remark());
        evaluate(a, calendarService.shiftOf(e), calendarService.isWorkday(req.workDate()));
        if (insert) {
            attendanceMapper.insert(a);
        } else {
            saveTimes(a);
        }
    }

    @Transactional
    public void delete(List<Long> ids) {
        attendanceMapper.selectByIds(ids).forEach(a -> payrollLock.checkOpen(a.getWorkDate(), "hr.label.deleteAttendanceRecords"));
        attendanceMapper.deleteByIds(ids);
    }

    /**
     * 登录账号对应员工某一天的请假、加班和出勤（项目工时校验）
     *
     * @return 账号没有关联员工时返回 null
     */
    public DayWork dayWork(Long userId, LocalDate date) {
        HrEmployee e = employeeMapper.selectOne(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getUserId, userId));
        if (e == null) {
            return null;
        }
        BigDecimal dayHours = ruleService.current().dayHours();
        BigDecimal leave = BigDecimal.ZERO;
        BigDecimal overtime = BigDecimal.ZERO;
        for (HrLeave l : leaveMapper.selectApprovedOverlapping(List.of(e.getId()), date.atStartOfDay(), date.plusDays(1).atStartOfDay())) {
            LocalDate start = l.getStartTime().toLocalDate();
            LocalDate end = l.getEndTime().toLocalDate();
            // 当天开始并结束的按申请时长，跨天的按全天（不超过每日工时，默认 8 小时）
            BigDecimal hours = start.equals(date) && end.equals(date) ? l.getHours() : l.getHours().min(dayHours);
            if (start.isBefore(date) && end.isAfter(date)) {
                hours = dayHours;
            }
            if (l.getKind() == LeaveKind.OVERTIME) {
                overtime = overtime.add(hours);
            } else {
                leave = leave.add(hours);
            }
        }
        HrAttendance a = attendanceMapper.selectOne(Wrappers.<HrAttendance>lambdaQuery()
                .eq(HrAttendance::getEmployeeId, e.getId()).eq(HrAttendance::getWorkDate, date));
        return new DayWork(leave.min(dayHours), overtime, a == null ? null : a.getWorkHours());
    }

    /**
     * 月度汇总（考勤页面，按数据权限）
     *
     * @param period yyyyMM
     */
    @DataPermission(table = "hr_employee", deptColumn = "dept_id", userColumn = "user_id")
    public List<AttendanceSummary> summaryPage(int period, Long deptId, String keyword) {
        YearMonth ym = toYearMonth(period);
        List<HrEmployee> employees = employeeMapper.selectEmployedBetween(ym.atDay(1), ym.atEndOfMonth(),
                deptId == null ? null : deptService.selfAndDescendantIds(deptId), keyword);
        return summarize(employees, ym);
    }

    /** 员工的月度汇总（员工自助、考勤页面） */
    public List<AttendanceSummary> summarize(List<HrEmployee> employees, YearMonth ym) {
        return summarize(employees, ym, ruleService.current());
    }

    /** 员工的月度汇总，加班倍数按给定的规则（工资计算） */
    public List<AttendanceSummary> summarize(List<HrEmployee> employees, YearMonth ym, HrRules rules) {
        if (employees.isEmpty()) {
            return List.of();
        }
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();
        Set<Long> ids = employees.stream().map(HrEmployee::getId).collect(Collectors.toSet());
        Map<LocalDate, HrHoliday> holidays = calendarService.holidayMap(first, last);
        List<LocalDate> monthWorkdays = HrCalendarService.workdays(first, last, holidays);
        Map<Long, Map<LocalDate, HrAttendance>> records = attendanceMapper.selectBetween(ids, first, last).stream()
                .collect(Collectors.groupingBy(HrAttendance::getEmployeeId, Collectors.toMap(HrAttendance::getWorkDate, a -> a)));
        Map<Long, List<HrLeave>> leaves = leaveMapper.selectApprovedOverlapping(ids, first.atStartOfDay(), last.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(HrLeave::getEmployeeId));

        List<AttendanceSummary> result = new ArrayList<>();
        for (HrEmployee e : employees) {
            LocalDate from = e.getEntryDate().isAfter(first) ? e.getEntryDate() : first;
            LocalDate to = e.getLeaveDate() != null && e.getLeaveDate().isBefore(last) ? e.getLeaveDate() : last;
            Map<LocalDate, HrAttendance> mine = records.getOrDefault(e.getId(), Map.of());
            List<HrLeave> myLeaves = leaves.getOrDefault(e.getId(), List.of());
            int should = 0, actual = 0, late = 0, early = 0, missing = 0, absent = 0;
            for (LocalDate d : monthWorkdays) {
                if (d.isBefore(from) || d.isAfter(to)) {
                    continue;
                }
                should++;
                HrAttendance a = mine.get(d);
                if (a != null) {
                    actual++;
                    AttendanceStatus status = a.getStatus();
                    if (status.isLate()) {
                        late++;
                    }
                    if (status.isEarly()) {
                        early++;
                    }
                    if (status == AttendanceStatus.MISSING && d.isBefore(today)) {
                        missing++;
                    }
                } else if (d.isBefore(today) && !onLeave(myLeaves, d)) {
                    absent++;
                }
            }
            Map<String, BigDecimal> leaveHours = new HashMap<>();
            BigDecimal overtime = BigDecimal.ZERO;
            BigDecimal weighted = BigDecimal.ZERO;
            for (HrLeave l : myLeaves) {
                // 按开始时间归属月份
                if (!YearMonth.from(l.getStartTime()).equals(ym)) {
                    continue;
                }
                if (l.getKind() == LeaveKind.OVERTIME) {
                    // 转调休的加班不发加班费
                    if (OvertimeComp.of(l.getType()) == OvertimeComp.TO_LEAVE) {
                        continue;
                    }
                    overtime = overtime.add(l.getHours());
                    weighted = weighted.add(l.getHours().multiply(overtimeRate(HrCalendarService.dayType(l.getStartTime().toLocalDate(), holidays), rules)));
                } else {
                    leaveHours.merge(l.getType(), l.getHours(), BigDecimal::add);
                }
            }
            result.add(new AttendanceSummary(e.getId(), e.getEmpNo(), e.getName(), e.getDeptId(), e.getDeptName(),
                    monthWorkdays.size(), should, actual, late, early, missing, absent, leaveHours, overtime, weighted));
        }
        return result;
    }

    /** 员工自助：本月考勤日历 */
    public List<CalendarDay> calendar(HrEmployee e, YearMonth ym) {
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();
        Map<LocalDate, HrHoliday> holidays = calendarService.holidayMap(first, last);
        Map<LocalDate, HrAttendance> records = attendanceMapper.selectBetween(Set.of(e.getId()), first, last).stream()
                .collect(Collectors.toMap(HrAttendance::getWorkDate, a -> a));
        List<HrLeave> leaves = leaveMapper.selectList(Wrappers.<HrLeave>lambdaQuery()
                .eq(HrLeave::getEmployeeId, e.getId())
                .in(HrLeave::getStatus, LeaveStatus.EFFECTIVE)
                .lt(HrLeave::getStartTime, last.plusDays(1).atStartOfDay())
                .gt(HrLeave::getEndTime, first.atStartOfDay()));
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            LocalDate day = d;
            boolean workday = HrCalendarService.isWorkday(d, holidays);
            HrHoliday h = holidays.get(d);
            List<HrLeave> dayLeaves = leaves.stream().filter(l -> covers(l, day)).toList();
            boolean employed = !d.isBefore(e.getEntryDate()) && (e.getLeaveDate() == null || !d.isAfter(e.getLeaveDate()));
            boolean absent = workday && employed && d.isBefore(today) && !records.containsKey(d)
                    && dayLeaves.stream().noneMatch(l -> l.getKind() == LeaveKind.LEAVE && l.getStatus() == LeaveStatus.APPROVED);
            days.add(new CalendarDay(d, workday, h == null ? null : h.getName(), records.get(d), dayLeaves, absent));
        }
        return days;
    }

    // ---------------------------------------------------------------- 内部

    /** 按班次计算迟到、早退、缺卡、出勤时长；非工作日打卡（加班）不计迟到早退 */
    static void evaluate(HrAttendance a, HrShift shift, boolean workday) {
        int late = 0, early = 0;
        if (workday) {
            LocalTime lateAfter = shift.getStartTime().plusMinutes(shift.getFlexMinutes());
            LocalTime earlyBefore = shift.getEndTime().minusMinutes(shift.getFlexMinutes());
            if (a.getCheckIn() != null && a.getCheckIn().toLocalTime().isAfter(lateAfter)) {
                late = (int) Duration.between(shift.getStartTime(), a.getCheckIn().toLocalTime()).toMinutes();
            }
            if (a.getCheckOut() != null && a.getCheckOut().toLocalTime().isBefore(earlyBefore)) {
                early = (int) Duration.between(a.getCheckOut().toLocalTime(), shift.getEndTime()).toMinutes();
            }
        }
        a.setLateMinutes(late);
        a.setEarlyMinutes(early);
        if (a.getCheckIn() == null || a.getCheckOut() == null) {
            a.setStatus(AttendanceStatus.MISSING);
            a.setWorkHours(BigDecimal.ZERO);
            return;
        }
        a.setStatus(AttendanceStatus.of(late, early));
        long minutes = Duration.between(a.getCheckIn(), a.getCheckOut()).toMinutes();
        if (minutes > LUNCH_THRESHOLD_MINUTES) {
            minutes -= 60;
        }
        a.setWorkHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
    }

    /** 加班倍数（业务参数，默认工作日 1.5、休息日 2、法定节假日 3） */
    static BigDecimal overtimeRate(HrCalendarService.DayType type, HrRules rules) {
        return switch (type) {
            case WORKDAY -> rules.overtimeWorkday();
            case RESTDAY -> rules.overtimeRestday();
            case HOLIDAY -> rules.overtimeHoliday();
        };
    }

    /** 请假覆盖当天（任意时段） */
    private static boolean covers(HrLeave l, LocalDate d) {
        return l.getStartTime().isBefore(d.plusDays(1).atStartOfDay()) && l.getEndTime().isAfter(d.atStartOfDay());
    }

    private static boolean onLeave(List<HrLeave> leaves, LocalDate d) {
        return leaves.stream().anyMatch(l -> l.getKind() == LeaveKind.LEAVE && covers(l, d));
    }

    private HrAttendance find(Long employeeId, LocalDate date) {
        return attendanceMapper.selectOne(Wrappers.<HrAttendance>lambdaQuery()
                .eq(HrAttendance::getEmployeeId, employeeId).eq(HrAttendance::getWorkDate, date));
    }

    private void saveTimes(HrAttendance a) {
        attendanceMapper.update(Wrappers.<HrAttendance>lambdaUpdate()
                .set(HrAttendance::getCheckIn, a.getCheckIn())
                .set(HrAttendance::getCheckOut, a.getCheckOut())
                .set(HrAttendance::getStatus, a.getStatus())
                .set(HrAttendance::getLateMinutes, a.getLateMinutes())
                .set(HrAttendance::getEarlyMinutes, a.getEarlyMinutes())
                .set(HrAttendance::getWorkHours, a.getWorkHours())
                .set(HrAttendance::getSource, a.getSource())
                .set(HrAttendance::getRemark, a.getRemark())
                .set(HrAttendance::getUpdateTime, LocalDateTime.now())
                .eq(HrAttendance::getId, a.getId()));
    }

    static YearMonth toYearMonth(int period) {
        int month = period % 100;
        if (month < 1 || month > 12 || period < 190001 || period > 299912) {
            throw new BizException("hr.error.invalidMonthFormat", period);
        }
        return YearMonth.of(period / 100, month);
    }
}
