package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.HrRules;

import java.math.BigDecimal;

/**
 * 薪资档案页面估算用到的当前租户计算规则（业务参数的只读子集，有薪资档案查看权限即可读取）
 *
 * @param monthPayDays   月计薪天数（默认 21.75）
 * @param dayHours       每日工时（默认 8）
 * @param monthPayHours  月计薪小时 = 月计薪天数 × 每日工时（默认 174），小时人工成本 = 月度人工成本 / 月计薪小时
 * @param socialPersonal 默认个人社保比例（%，默认 10.5），员工没有社保方案时使用
 * @param socialCompany  默认单位社保比例（%，默认 26），员工没有社保方案时使用
 */
public record SalaryRules(BigDecimal monthPayDays, BigDecimal dayHours, BigDecimal monthPayHours,
                          BigDecimal socialPersonal, BigDecimal socialCompany) {

    public static SalaryRules of(HrRules rules) {
        return new SalaryRules(rules.monthPayDays(), rules.dayHours(), rules.monthPayHours(),
                rules.socialPersonal(), rules.socialCompany());
    }
}
