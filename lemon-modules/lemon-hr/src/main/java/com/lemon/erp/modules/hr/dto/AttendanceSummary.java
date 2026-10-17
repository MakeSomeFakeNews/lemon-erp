package com.lemon.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 员工月度考勤汇总
 *
 * @param monthDays   本月工作日天数
 * @param shouldDays  应出勤天数（在职期间的工作日，当月只统计到今天）
 * @param actualDays  实际出勤天数（工作日有打卡）
 * @param absentDays  旷工天数：应出勤日既没有打卡也没有请假
 * @param leaveHours  已通过的请假小时，按假期类型
 * @param overtimeHours 已通过的加班小时
 * @param overtimeWeighted 加班小时 × 倍数（业务参数，默认工作日 1.5、休息日 2、节假日 3）
 */
public record AttendanceSummary(Long employeeId, String empNo, String employeeName, Long deptId, String deptName,
                                int monthDays, int shouldDays, int actualDays, int lateCount, int earlyCount, int missingCount,
                                int absentDays, Map<String, BigDecimal> leaveHours, BigDecimal overtimeHours,
                                BigDecimal overtimeWeighted) {

    public BigDecimal leaveHoursOf(String type) {
        return leaveHours.getOrDefault(type, BigDecimal.ZERO);
    }
}
