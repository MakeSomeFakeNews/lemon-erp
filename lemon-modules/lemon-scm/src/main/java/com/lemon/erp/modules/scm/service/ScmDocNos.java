package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;
import static com.lemon.erp.framework.code.DocNoCatalog.Type.master;

/**
 * 供应链模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致；名称为消息键
 */
@Configuration
public class ScmDocNos {

    /** 供应商、客户编码规则标识：新增时编码留空则自动生成 */
    public static final String SUPPLIER = "GYS";
    public static final String CUSTOMER = "KH";

    @Bean
    @Order(1)
    public DocNoCatalog scmDocNoCatalog() {
        List<DocNoCatalog.Type> types = new ArrayList<>();
        types.add(doc("QG", "scm.label.purchaseRequisition"));
        types.add(doc("XJ", "scm.label.rfq"));
        types.add(doc(ScmTenderService.DOC_NO, "scm.label.tender"));
        types.add(doc(ScmPurchaseContractService.DOC_NO, "scm.label.purchaseContract"));
        types.add(doc("BJ", "scm.label.salesQuotation"));
        Arrays.stream(OrderKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabelKey())));
        types.add(doc("BG", "scm.label.orderChange"));
        types.add(doc(ScmAsnService.DOC_NO, "scm.label.asn"));
        Arrays.stream(TradeKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabelKey())));
        types.add(doc("FH", "scm.label.deliveryNotice"));
        types.add(doc(ScmWaveService.DOC_NO, "scm.label.pickingWave"));
        types.add(doc("FY", "scm.label.shipment"));
        types.add(doc(ScmRebateService.DOC_NO, "scm.label.salesRebate"));
        types.add(master(SUPPLIER, "scm.label.supplierCode"));
        types.add(master(CUSTOMER, "scm.label.customerCode"));
        return new DocNoCatalog("scm.label.supplyChainManagement", types);
    }
}
