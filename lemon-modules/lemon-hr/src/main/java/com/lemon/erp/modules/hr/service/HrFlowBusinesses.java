package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.modules.hr.model.LeaveKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 人力资源模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 发起审批时传入的变量保持一致
 */
@Configuration
public class HrFlowBusinesses {

    @Bean
    public FlowBusiness leaveFlow() {
        return FlowBusiness.of("hr_leave", "hr.label.leaveOvertimeApproval",
                dict("kind", "hr.label.requestKind", "hr_leave_kind"),
                dict("type", "hr.label.leaveTypeOvertimeOvertime", "hr_leave_type", LeaveKind.OVERTIME.name()),
                number("days", "hr.label.days"),
                number("hours", "hr.label.hours"),
                dict("compensation", "hr.label.overtimeCompensation", "hr_overtime_comp"));
    }

    @Bean
    public FlowBusiness recruitFlow() {
        return FlowBusiness.of("hr_recruit", "hr.label.jobRequisitionApproval",
                number("headcount", "hr.label.headcount"),
                text("recruitNo", "hr.label.requisitionNo"));
    }

    @Bean
    public FlowBusiness changeFlow() {
        return FlowBusiness.of("hr_change", "hr.label.personnelChangeApproval",
                dict("type", "hr.label.changeType", "hr_change_type"),
                number("salary", "hr.label.newSalary"),
                number("salaryIncrease", "hr.label.salaryChangeCny"),
                text("employeeName", "hr.label.employeeName"));
    }

    @Bean
    public FlowBusiness payrollFlow() {
        return FlowBusiness.of("hr_payroll", "hr.label.payrollApproval",
                number("amount", "hr.label.grossPayTotal"),
                number("headcount", "hr.label.headcount2"),
                text("period", "hr.label.payrollMonth"));
    }
}
