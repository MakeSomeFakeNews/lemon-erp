package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.entity.HrEmployee;

import java.math.BigDecimal;

/**
 * 员工自助首页：本人档案、年假余额、本年培训学时、待办数量
 *
 * @param annualDays    本年年假天数
 * @param annualUsedDays 本年已用（含审批中）年假天数
 * @param pendingReviews 待我评分的考核表数
 * @param compensatoryHours 调休余额（小时）
 */
public record SelfProfile(HrEmployee employee, int annualDays, BigDecimal annualUsedDays, BigDecimal trainingHours,
                          long pendingSelf, long pendingReviews, BigDecimal compensatoryHours) {
}
