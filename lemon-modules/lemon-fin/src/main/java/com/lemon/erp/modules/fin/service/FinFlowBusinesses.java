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
        return FlowBusiness.of("fin_voucher", "fin.label.voucherApproval",
                number("amount", "fin.label.totalDebit"),
                dict("sourceType", "fin.label.voucherSource", "fin_voucher_source"),
                text("voucherNo", "fin.label.voucherNo"));
    }

    @Bean
    public FlowBusiness paymentFlow() {
        return FlowBusiness.of("fin_payment", "fin.label.paymentApproval",
                number("amount", "fin.label.paymentAmount"),
                text("settleNo", "fin.label.paymentNo"));
    }

    @Bean
    public FlowBusiness expenseFlow() {
        return FlowBusiness.of("fin_expense", "fin.label.expenseClaimApproval",
                number("amount", "fin.label.claimAmount"),
                text("claimNo", "fin.label.claimNo"));
    }

    @Bean
    public FlowBusiness loanFlow() {
        return FlowBusiness.of("fin_loan", "fin.label.employeeLoanApproval",
                number("amount", "fin.label.loanAmount"),
                text("loanNo", "fin.label.loanNo"));
    }
}
