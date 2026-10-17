package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 生产模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class MesFlowBusinesses {

    @Bean
    public FlowBusiness supplementFlow() {
        return FlowBusiness.of("mes_supplement", "mes.label.supplementaryIssueApproval",
                dict("reason", "mes.label.supplementaryIssueReason", "mes_supplement_reason"),
                number("totalQty", "mes.label.totalSupplementaryQty"),
                text("woNo", "mes.label.workOrderNo"));
    }

    /** 变量与 {@link MesEcnService#submit} 发起时传入的一致 */
    @Bean
    public FlowBusiness ecnFlow() {
        return FlowBusiness.of(MesEcnService.PROCESS_KEY, "mes.label.ecnApproval",
                dict("reason", "mes.label.changeReason", "mes_ecn_reason"),
                text("materialCode", "mes.label.productCode"),
                text("ecnNo", "mes.label.ecnNo"));
    }

    /** 变量与 {@link QmsNcrService#review} 发起时传入的一致 */
    @Bean
    public FlowBusiness ncrFlow() {
        return FlowBusiness.of(QmsNcrService.PROCESS_KEY, "mes.label.nonconformanceDispositionApproval",
                number("qty", "mes.label.nonconformingQty"),
                dict("disposition", "mes.label.disposition", "qms_disposition"),
                dict("responsibility", "mes.label.responsibility", "qms_responsibility"),
                text("ncrNo", "mes.label.ncrNo"));
    }
}
