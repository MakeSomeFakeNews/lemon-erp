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
        return FlowBusiness.of("pm_project", "项目立项审批",  // i18n-ignore 流程模块输出处翻译
                number("contractAmount", "合同金额"),  // i18n-ignore 流程模块输出处翻译
                number("budgetAmount", "成本预算"),  // i18n-ignore 流程模块输出处翻译
                dict("type", "项目类型", "pm_project_type"),  // i18n-ignore 流程模块输出处翻译
                text("projectCode", "项目编号"));  // i18n-ignore 流程模块输出处翻译
    }

    @Bean
    public FlowBusiness costFlow() {
        return FlowBusiness.of("pm_cost", "项目费用审批",  // i18n-ignore 流程模块输出处翻译
                number("amount", "费用金额"),  // i18n-ignore 流程模块输出处翻译
                dict("costType", "费用类型", "pm_cost_type"));  // i18n-ignore 流程模块输出处翻译
    }
}
