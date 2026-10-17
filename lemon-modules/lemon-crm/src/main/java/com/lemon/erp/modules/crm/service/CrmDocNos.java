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

    /** 销售线索 */
    public static final String LEAD = "XL";
    /** 商机 */
    public static final String OPPORTUNITY = "SJ";
    /** 销售合同 */
    public static final String CONTRACT = "XSHT";
    /** 服务工单 */
    public static final String TICKET = "FW";

    @Bean
    @Order(6)
    public DocNoCatalog crmDocNoCatalog() {
        return DocNoCatalog.of("crm.label.crm",
                doc(LEAD, "crm.label.lead"),
                doc(OPPORTUNITY, "crm.label.opportunity"),
                doc(CONTRACT, "crm.label.salesContract"),
                doc(TICKET, "crm.label.serviceTicket"));
    }
}
