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

    /** 劳动合同 */
    public static final String CONTRACT = "HT";
    /** 招聘需求 */
    public static final String RECRUIT = "ZP";
    /** 请假 / 加班申请 */
    public static final String LEAVE = "QJ";
    /** 工资表 */
    public static final String PAYROLL = "GZ";

    @Bean
    @Order(7)
    public DocNoCatalog hrDocNoCatalog() {
        return DocNoCatalog.of("hr.label.humanResources",
                doc(CONTRACT, "hr.label.employmentContract"),
                doc(RECRUIT, "hr.label.jobRequisition"),
                doc(LEAVE, "hr.label.leaveOvertimeRequest"),
                doc(PAYROLL, "hr.label.payroll"));
    }
}
