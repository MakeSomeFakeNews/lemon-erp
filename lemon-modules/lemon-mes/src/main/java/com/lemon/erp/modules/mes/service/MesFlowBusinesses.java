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
        return FlowBusiness.of("mes_supplement", "补料申请审批",
                dict("reason", "补料原因", "mes_supplement_reason"),
                number("totalQty", "补料总数量"),
                text("woNo", "工单号"));
    }

    /** 变量与 {@link MesEcnService#submit} 发起时传入的一致 */
    @Bean
    public FlowBusiness ecnFlow() {
        return FlowBusiness.of(MesEcnService.PROCESS_KEY, "工程变更审批",
                dict("reason", "变更原因", "mes_ecn_reason"),
                text("materialCode", "产品编码"),
                text("ecnNo", "变更单号"));
    }

    /** 变量与 {@link QmsNcrService#review} 发起时传入的一致 */
    @Bean
    public FlowBusiness ncrFlow() {
        return FlowBusiness.of(QmsNcrService.PROCESS_KEY, "不合格品处置审批",
                number("qty", "不合格数量"),
                dict("disposition", "处置方式", "qms_disposition"),
                dict("responsibility", "责任方", "qms_responsibility"),
                text("ncrNo", "处理单号"));
    }
}
