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
        return DocNoCatalog.of("hr.label.humanResources",
                doc("HT", "hr.label.employmentContract"),
                doc("ZP", "hr.label.jobRequisition"),
                doc("QJ", "hr.label.leaveOvertimeRequest"),
                doc("GZ", "hr.label.payroll"));
    }
}
