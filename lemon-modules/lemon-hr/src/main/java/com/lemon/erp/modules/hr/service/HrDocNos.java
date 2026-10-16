package com.lemon.erp.modules.hr.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 人力资源模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class HrDocNos {

    @Bean
    @Order(7)
    public DocNoCatalog hrDocNoCatalog() {
        return DocNoCatalog.of("人力资源",
                doc("HT", "劳动合同"),
                doc("ZP", "招聘需求"),
                doc("QJ", "假勤申请"),
                doc("GZ", "工资表"));
    }
}
