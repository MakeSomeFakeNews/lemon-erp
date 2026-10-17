package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.modules.scm.model.OrderKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 供应链模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致；流程名称、变量名称为消息键
 */
@Configuration
public class ScmFlowBusinesses {

    @Bean
    public FlowBusiness purchaseOrderFlow() {
        return FlowBusiness.of("scm_purchase_order", "scm.label.purchaseOrderApproval",
                number("amount", "scm.label.orderAmount"),
                text("orderNo", "scm.label.orderNo"));
    }

    /** 变量与 {@link ScmPurchaseContractService#submit} 发起时传入的一致 */
    @Bean
    public FlowBusiness purchaseContractFlow() {
        return FlowBusiness.of(ScmPurchaseContractService.PROCESS_KEY, "scm.label.purchaseContractApproval",
                number("amount", "scm.label.contractAmount"),
                dict("contractType", "scm.label.contractType", "scm_contract_type"),
                text("contractNo", "scm.label.contractNo"));
    }

    @Bean
    public FlowBusiness salesOrderFlow() {
        return FlowBusiness.of("scm_sales_order", "scm.label.salesOrderApproval",
                number("amount", "scm.label.orderAmount"),
                text("orderNo", "scm.label.orderNo"));
    }

    @Bean
    public FlowBusiness requisitionFlow() {
        return FlowBusiness.of("scm_requisition", "scm.label.purchaseRequisitionApproval",
                text("reqNo", "scm.label.requisitionNo"));
    }

    @Bean
    public FlowBusiness quotationFlow() {
        return FlowBusiness.of("scm_quotation", "scm.label.salesQuotationApproval",
                number("totalAmount", "scm.label.quotationAmount"),
                number("discountRate", "scm.label.lowestDiscountRateEGMeans"),
                text("quoteNo", "scm.label.quotationNo"));
    }

    @Bean
    public FlowBusiness orderChangeFlow() {
        return FlowBusiness.of("scm_order_change", "scm.label.orderChangeApproval",
                select("kind", "scm.label.orderType", OrderKind.values()),
                number("amountDelta", "scm.label.changeAmountDifferencePositiveIncrease"),
                text("orderNo", "scm.label.orderNo"));
    }
}
