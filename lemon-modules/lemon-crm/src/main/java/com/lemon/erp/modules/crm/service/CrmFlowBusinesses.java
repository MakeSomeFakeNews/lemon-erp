package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 客户关系模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class CrmFlowBusinesses {

    @Bean
    public FlowBusiness contractFlow() {
        return FlowBusiness.of("crm_contract", "销售合同审批", // i18n-ignore 流程模块输出处翻译
                number("amount", "合同金额"), // i18n-ignore 流程模块输出处翻译
                text("contractNo", "合同编号")); // i18n-ignore 流程模块输出处翻译
    }
}
