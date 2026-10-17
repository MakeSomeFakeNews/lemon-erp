package com.lemon.erp.modules.pm.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 项目模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class PmFlowBusinesses {

    @Bean
    public FlowBusiness projectFlow() {
        return FlowBusiness.of("pm_project", "pm.label.projectInitiationApproval",
                number("contractAmount", "pm.label.contractAmount"),
                number("budgetAmount", "pm.label.costBudget"),
                dict("type", "pm.label.projectType", "pm_project_type"),
                text("projectCode", "pm.label.projectCode"));
    }

    @Bean
    public FlowBusiness costFlow() {
        return FlowBusiness.of("pm_cost", "pm.label.projectExpenseApproval",
                number("amount", "pm.label.expenseAmount"),
                dict("costType", "pm.label.expenseType", "pm_cost_type"));
    }
}
