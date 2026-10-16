package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 财务模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class FinDocNos {

    @Bean
    @Order(5)
    public DocNoCatalog finDocNoCatalog() {
        return DocNoCatalog.of("财务管理",
                doc("YS", "应收单"),
                doc("YF", "应付单"),
                doc("SK", "收款单"),
                doc("FK", "付款单"),
                doc("YSK", "预收款"),
                doc("YFK", "预付款"),
                doc("BX", "费用报销"),
                doc("JK", "员工借款"),
                doc(FinFundService.TRANSFER_NO, "账户调拨"),
                doc(FinStatementService.DOC_NO, "供应商对账单"),
                doc(FinAssetCheckService.DOC_NO, "资产盘点"),
                doc(FinConsumableService.DOC_NO, "低值易耗品领用"));
    }
}
