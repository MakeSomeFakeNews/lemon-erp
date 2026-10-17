package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.modules.wms.model.StockBizType;
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
        return FlowBusiness.of("wms_check", "wms.label.stockCountDifferenceApproval",
                number("gainQty", "wms.label.gainQty"),
                number("lossQty", "wms.label.lossQty"),
                number("diffLines", "wms.label.differenceLines"),
                text("docNo", "wms.label.stockCountNo"));
    }

    @Bean
    public FlowBusiness stockDocFlow() {
        return FlowBusiness.of("wms_stock_doc", "wms.label.inboundOutboundConfirmationApproval",
                select("kind", "wms.label.documentType", StockBizType.IN, StockBizType.OUT, StockBizType.TRANSFER),
                text("bizType", "wms.label.businessTypeEGPurchaseSales"),
                number("totalQty", "wms.label.totalQty"),
                text("docNo", "wms.label.documentNo"));
    }
}
