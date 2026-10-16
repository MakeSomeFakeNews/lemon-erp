package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.entity.HrLeave;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工自助的考勤日历
 *
 * @param workday     是否工作日
 * @param holidayName 节假日 / 调休名称
 * @param absent      应出勤但没有打卡和请假（不含今天之后）
 */
public record CalendarDay(LocalDate date, boolean workday, String holidayName, HrAttendance attendance,
                          List<HrLeave> leaves, boolean absent) {
}
