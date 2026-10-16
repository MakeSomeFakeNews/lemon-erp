package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.*;

/**
 * 仓库模块接入审批的流程及发起时传入的流程变量（流程设计器的条件分支可选），与各 Service 中 flowApi.start 传入的变量保持一致
 */
@Configuration
public class WmsFlowBusinesses {

    @Bean
    public FlowBusiness checkFlow() {
        return FlowBusiness.of("wms_check", "盘点差异审批",
                number("gainQty", "盘盈数量"),
                number("lossQty", "盘亏数量"),
                number("diffLines", "差异行数"),
                text("docNo", "盘点单号"));
    }

    @Bean
    public FlowBusiness stockDocFlow() {
        return FlowBusiness.of("wms_stock_doc", "出入库确认审批",
                select("kind", "单据类型", "IN", "OUT", "TRANSFER"),
                text("bizType", "业务类型（如 PURCHASE、SALES、OTHER）"),
                number("totalQty", "总数量"),
                text("docNo", "单号"));
    }
}
