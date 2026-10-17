package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.modules.wms.model.StockBizType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;
import static com.lemon.erp.framework.code.DocNoCatalog.Type.master;

/**
 * 仓库模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class WmsDocNos {

    /** 物料编码规则标识：新增物料时编码留空则自动生成 */
    public static final String MATERIAL = "WL";

    @Bean
    @Order(2)
    public DocNoCatalog wmsDocNoCatalog() {
        return DocNoCatalog.of("仓库管理",
                doc(StockBizType.IN.getPrefix(), StockBizType.IN.getLabel()),
                doc(StockBizType.OUT.getPrefix(), StockBizType.OUT.getLabel()),
                doc(StockBizType.TRANSFER.getPrefix(), StockBizType.TRANSFER.getLabel()),
                doc(StockBizType.CHECK.getPrefix(), StockBizType.CHECK.getLabel()),
                doc(WmsAssemblyService.DOC_NO, "组装拆卸单"),
                master(MATERIAL, "物料编码"));
    }
}
