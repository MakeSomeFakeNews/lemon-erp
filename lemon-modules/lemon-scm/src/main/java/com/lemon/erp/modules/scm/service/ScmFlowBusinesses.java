package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 供应链模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class ScmFlowBusinesses {

    @Bean
    public FlowBusiness purchaseOrderFlow() {
        return FlowBusiness.of("scm_purchase_order", "采购订单审核",
                number("amount", "订单金额"),
                text("orderNo", "订单号"));
    }

    /** 变量与 {@link ScmPurchaseContractService#submit} 发起时传入的一致 */
    @Bean
    public FlowBusiness purchaseContractFlow() {
        return FlowBusiness.of(ScmPurchaseContractService.PROCESS_KEY, "采购合同审批",
                number("amount", "合同金额"),
                dict("contractType", "合同类型", "scm_contract_type"),
                text("contractNo", "合同编号"));
    }

    @Bean
    public FlowBusiness salesOrderFlow() {
        return FlowBusiness.of("scm_sales_order", "销售订单审核",
                number("amount", "订单金额"),
                text("orderNo", "订单号"));
    }

    @Bean
    public FlowBusiness requisitionFlow() {
        return FlowBusiness.of("scm_requisition", "请购单审批",
                text("reqNo", "请购单号"));
    }

    @Bean
    public FlowBusiness quotationFlow() {
        return FlowBusiness.of("scm_quotation", "销售报价审批",
                number("totalAmount", "报价金额"),
                number("discountRate", "最低折扣率（%，如 85 表示 85 折）"),
                text("quoteNo", "报价单号"));
    }

    @Bean
    public FlowBusiness orderChangeFlow() {
        return FlowBusiness.of("scm_order_change", "订单变更审批",
                select("kind", "订单类型", "PURCHASE", "SALES"),
                number("amountDelta", "变更金额差额（增加为正）"),
                text("orderNo", "订单号"));
    }
}
