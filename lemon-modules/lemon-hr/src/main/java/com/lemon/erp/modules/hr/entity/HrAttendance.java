package com.lemon.erp.modules.hr.entity;

import com.lemon.erp.modules.hr.model.AttendanceSource;
import com.lemon.erp.modules.hr.model.AttendanceStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录（每人每天一条）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance")
public class HrAttendance extends BaseEntity {

    /** 员工 */
    private Long employeeId;

    /** 日期 */
    private LocalDate workDate;

    /** 上班打卡 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkIn;

    /** 下班打卡 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOut;

    /** 状态(字典 hr_attendance_status) NORMAL正常 LATE迟到 EARLY早退 LATE_EARLY迟到早退 MISSING缺卡 */
    private AttendanceStatus status;

    /** 迟到分钟 */
    private Integer lateMinutes;

    /** 早退分钟 */
    private Integer earlyMinutes;

    /** 出勤时长（小时） */
    private BigDecimal workHours;

    /** 来源 CLOCK打卡 MANUAL补录 */
    private AttendanceSource source;

    /** 补录说明 */
    private String remark;

    @TableField(exist = false)
    private String empNo;
    @TableField(exist = false)
    private String employeeName;
    @TableField(exist = false)
    private String deptName;
}
