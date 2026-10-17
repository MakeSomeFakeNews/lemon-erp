package com.lemon.erp.modules.crm.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 客户关系模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class CrmDocNos {

    @Bean
    @Order(6)
    public DocNoCatalog crmDocNoCatalog() {
        return DocNoCatalog.of("crm.label.crm",
                doc("XL", "crm.label.lead"),
                doc("SJ", "crm.label.opportunity"),
                doc("XSHT", "crm.label.salesContract"),
                doc("FW", "crm.label.serviceTicket"));
    }
}
