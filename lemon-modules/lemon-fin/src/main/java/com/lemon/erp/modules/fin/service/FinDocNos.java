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

    /** 应收单、应付单 */
    public static final String RECEIVABLE = "YS";
    public static final String PAYABLE = "YF";
    /** 收款单、付款单 */
    public static final String RECEIPT = "SK";
    public static final String PAYMENT = "FK";
    /** 预收单、预付单 */
    public static final String ADVANCE_RECEIPT = "YSK";
    public static final String ADVANCE_PAYMENT = "YFK";
    /** 费用报销单 */
    public static final String EXPENSE_CLAIM = "BX";
    /** 员工借款单 */
    public static final String LOAN = "JK";

    @Bean
    @Order(5)
    public DocNoCatalog finDocNoCatalog() {
        return DocNoCatalog.of("fin.label.financialManagement",
                doc(RECEIVABLE, "fin.label.receivable"),
                doc(PAYABLE, "fin.label.payable"),
                doc(RECEIPT, "fin.label.receipt"),
                doc(PAYMENT, "fin.label.payment"),
                doc(ADVANCE_RECEIPT, "fin.label.advanceReceipt"),
                doc(ADVANCE_PAYMENT, "fin.label.advancePayment"),
                doc(EXPENSE_CLAIM, "fin.label.expenseClaim"),
                doc(LOAN, "fin.label.employeeLoan"),
                doc(FinFundService.TRANSFER_NO, "fin.label.fundTransfer"),
                doc(FinStatementService.DOC_NO, "fin.label.supplierStatement"),
                doc(FinAssetCheckService.DOC_NO, "fin.label.assetCount"),
                doc(FinConsumableService.DOC_NO, "fin.label.consumableRequisition"));
    }
}
