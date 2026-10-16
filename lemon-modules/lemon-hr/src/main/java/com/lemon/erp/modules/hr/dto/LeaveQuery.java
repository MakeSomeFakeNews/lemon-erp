package com.lemon.erp.modules.hr.dto;

import java.time.LocalDate;

/**
 * @param keyword   单号、工号或姓名
 * @param beginDate 开始时间不早于该日期
 * @param endBefore 开始时间早于该日期（查询截止日期的次日）
 */
public record LeaveQuery(String keyword, String kind, String type, String status, Long deptId, LocalDate beginDate, LocalDate endBefore) {
}
