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
        return DocNoCatalog.of("客户关系", // i18n-ignore 编码规则目录名称，输出处翻译
                doc("XL", "线索"), // i18n-ignore 编码规则目录名称，输出处翻译
                doc("SJ", "销售机会"), // i18n-ignore 编码规则目录名称，输出处翻译
                doc("XSHT", "销售合同"), // i18n-ignore 编码规则目录名称，输出处翻译
                doc("FW", "服务工单")); // i18n-ignore 编码规则目录名称，输出处翻译
    }
}
