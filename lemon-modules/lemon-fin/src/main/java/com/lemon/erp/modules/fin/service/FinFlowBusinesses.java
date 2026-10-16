package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 财务模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class FinFlowBusinesses {

    @Bean
    public FlowBusiness voucherFlow() {
        return FlowBusiness.of("fin_voucher", "凭证审批",
                number("amount", "借方金额合计"),
                dict("sourceType", "凭证来源", "fin_voucher_source"),
                text("voucherNo", "凭证号"));
    }

    @Bean
    public FlowBusiness paymentFlow() {
        return FlowBusiness.of("fin_payment", "付款审批",
                number("amount", "付款金额"),
                text("settleNo", "付款单号"));
    }

    @Bean
    public FlowBusiness expenseFlow() {
        return FlowBusiness.of("fin_expense", "费用报销审批",
                number("amount", "报销金额"),
                text("claimNo", "报销单号"));
    }

    @Bean
    public FlowBusiness loanFlow() {
        return FlowBusiness.of("fin_loan", "员工借款审批",
                number("amount", "借款金额"),
                text("loanNo", "借款单号"));
    }
}
