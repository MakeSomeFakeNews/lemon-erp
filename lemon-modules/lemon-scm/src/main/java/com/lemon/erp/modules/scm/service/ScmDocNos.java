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
 * 供应链模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
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
        types.add(doc("QG", "请购单"));
        types.add(doc("XJ", "询价单"));
        types.add(doc(ScmTenderService.DOC_NO, "招标"));
        types.add(doc(ScmPurchaseContractService.DOC_NO, "采购合同"));
        types.add(doc("BJ", "销售报价"));
        Arrays.stream(OrderKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabel())));
        types.add(doc("BG", "订单变更"));
        types.add(doc(ScmAsnService.DOC_NO, "送货通知"));
        Arrays.stream(TradeKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabel())));
        types.add(doc("FH", "发货通知"));
        types.add(doc(ScmWaveService.DOC_NO, "拣货波次"));
        types.add(doc("FY", "物流发运"));
        types.add(doc(ScmRebateService.DOC_NO, "销售返利"));
        types.add(master(SUPPLIER, "供应商编码"));
        types.add(master(CUSTOMER, "客户编码"));
        return new DocNoCatalog("供应链管理", types);
    }
}
