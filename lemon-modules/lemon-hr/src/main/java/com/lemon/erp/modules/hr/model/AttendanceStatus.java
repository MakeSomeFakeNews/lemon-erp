package com.lemon.erp.modules.hr.model;

/**
 * 考勤状态：每日考勤结果（字典 hr_attendance_status）
 */
public enum AttendanceStatus {
    /** 正常 */
    NORMAL,
    /** 迟到 */
    LATE,
    /** 早退 */
    EARLY,
    /** 迟到早退 */
    LATE_EARLY,
    /** 缺卡 */
    MISSING;

    /** 按迟到、早退分钟数确定状态（上下班卡都有时） */
    public static AttendanceStatus of(int lateMinutes, int earlyMinutes) {
        if (lateMinutes > 0) {
            return earlyMinutes > 0 ? LATE_EARLY : LATE;
        }
        return earlyMinutes > 0 ? EARLY : NORMAL;
    }

    /** 是否计一次迟到 */
    public boolean isLate() {
        return this == LATE || this == LATE_EARLY;
    }

    /** 是否计一次早退 */
    public boolean isEarly() {
        return this == EARLY || this == LATE_EARLY;
    }
}
