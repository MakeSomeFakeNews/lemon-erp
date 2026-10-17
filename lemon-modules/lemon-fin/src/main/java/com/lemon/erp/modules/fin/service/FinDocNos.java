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
        return DocNoCatalog.of("fin.label.financialManagement",
                doc("YS", "fin.label.receivable"),
                doc("YF", "fin.label.payable"),
                doc("SK", "fin.label.receipt"),
                doc("FK", "fin.label.payment"),
                doc("YSK", "fin.label.advanceReceipt"),
                doc("YFK", "fin.label.advancePayment"),
                doc("BX", "fin.label.expenseClaim"),
                doc("JK", "fin.label.employeeLoan"),
                doc(FinFundService.TRANSFER_NO, "fin.label.fundTransfer"),
                doc(FinStatementService.DOC_NO, "fin.label.supplierStatement"),
                doc(FinAssetCheckService.DOC_NO, "fin.label.assetCount"),
                doc(FinConsumableService.DOC_NO, "fin.label.consumableRequisition"));
    }
}
