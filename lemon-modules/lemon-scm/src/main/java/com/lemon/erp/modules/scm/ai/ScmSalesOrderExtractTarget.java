package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 智能录入：销售订单（客户发来的采购单、订货单），客户、物料、仓库按名称匹配，数量单价检查 */
@Component
@Order(20) // 目标列表中的顺序
@RequiredArgsConstructor
public class ScmSalesOrderExtractTarget implements AiExtractTarget {

    private final ScmExtractSupport support;

    @Override
    public String key() {
        return "scm.sales-order";
    }

    @Override
    public String labelKey() {
        return "scm.label.salesOrder";
    }

    @Override
    public String permission() {
        return "scm:sales:add";
    }

    @Override
    public Map<String, Object> schema() {
        return support.orderSchema("customerName", "scm.ai.extractCustomerName");
    }

    @Override
    public String instructionKey() {
        return "scm.ai.extractSalesInstruction";
    }

    @Override
    public AiPrefill prefill(Map<String, Object> extracted) {
        return support.orderPrefill(extracted, false);
    }

    @Override
    public String route() {
        return "/scm/sales";
    }

    @Override
    public String form() {
        return "sales-order";
    }
}
