package com.lemon.erp.modules.hr.dto;

import java.math.BigDecimal;

/**
 * 员工某一天的考勤情况（项目工时校验使用）
 *
 * @param leaveHours    已通过的请假小时（跨天的请假中间日按全天 8 小时）
 * @param overtimeHours 已通过的加班小时
 * @param attendedHours 打卡出勤时长，没有考勤记录时为空
 */
public record DayWork(BigDecimal leaveHours, BigDecimal overtimeHours, BigDecimal attendedHours) {
}
