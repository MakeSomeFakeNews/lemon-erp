package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.ai.AiQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 问数工具按模型给出的文字（编码或名称）匹配客户、供应商（启用的），匹配不上或有多个时提示模型
 */
@Component
@RequiredArgsConstructor
public class ScmAiLookup {

    private final ScmCustomerService customerService;
    private final ScmSupplierService supplierService;

    /** 客户 ID，keyword 为空时返回 null */
    public Long customerId(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return AiQuerySupport.pick(customerService.options(keyword.trim()), keyword, PartnerOption::code, PartnerOption::name,
                "scm.error.aiCustomerNotFound", "scm.error.aiCustomerAmbiguous").id();
    }

    /** 供应商 ID，keyword 为空时返回 null */
    public Long supplierId(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return AiQuerySupport.pick(supplierService.options(keyword.trim()), keyword, PartnerOption::code, PartnerOption::name,
                "scm.error.aiSupplierNotFound", "scm.error.aiSupplierAmbiguous").id();
    }

    /** 订单种类对应的往来单位：销售为客户，采购为供应商 */
    public Long partnerId(OrderKind kind, String keyword) {
        return kind == OrderKind.SALES ? customerId(keyword) : supplierId(keyword);
    }
}
