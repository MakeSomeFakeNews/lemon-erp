package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.HrRules;

import java.math.BigDecimal;

/**
 * 请假申请估算时长用到的当前租户规则（业务参数的只读子集，员工自助登录即可读取）
 *
 * @param dayHours       每日工时：跨天请假每个工作日按此计（默认 8）
 * @param lunchThreshold 同一天内时长超过该小时数时扣除午休（默认 5）
 * @param lunchBreak     扣除的午休小时数（默认 1）
 */
public record LeaveRules(BigDecimal dayHours, BigDecimal lunchThreshold, BigDecimal lunchBreak) {

    public static LeaveRules of(HrRules rules) {
        return new LeaveRules(rules.dayHours(), rules.lunchThreshold(), rules.lunchBreak());
    }
}
