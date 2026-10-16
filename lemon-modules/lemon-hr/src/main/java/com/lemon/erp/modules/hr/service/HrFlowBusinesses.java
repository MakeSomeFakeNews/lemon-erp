package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 人力资源模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class HrFlowBusinesses {

    @Bean
    public FlowBusiness leaveFlow() {
        return FlowBusiness.of("hr_leave", "请假加班审批",
                dict("kind", "申请种类", "hr_leave_kind"),
                select("type", "请假类型（加班为 OVERTIME）", "ANNUAL", "PERSONAL", "SICK", "MARRIAGE", "MATERNITY", "COMPENSATORY", "OVERTIME"),
                number("days", "天数"),
                number("hours", "小时数"),
                dict("compensation", "加班补偿方式", "hr_overtime_comp"));
    }

    @Bean
    public FlowBusiness recruitFlow() {
        return FlowBusiness.of("hr_recruit", "招聘需求审批",
                number("headcount", "招聘人数"),
                text("recruitNo", "需求编号"));
    }

    @Bean
    public FlowBusiness changeFlow() {
        return FlowBusiness.of("hr_change", "人事异动审批",
                dict("type", "异动类型", "hr_change_type"),
                number("salary", "调整后薪资"),
                number("salaryIncrease", "调薪幅度（元）"),
                text("employeeName", "员工姓名"));
    }

    @Bean
    public FlowBusiness payrollFlow() {
        return FlowBusiness.of("hr_payroll", "工资表审批",
                number("amount", "应发合计"),
                number("headcount", "人数"),
                text("period", "工资月份"));
    }
}
