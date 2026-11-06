package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 智能录入：采购订单（供应商的报价单、送货单、我方的采购单），供应商、物料、仓库按名称匹配，数量单价检查 */
@Component
@Order(10) // 目标列表中的顺序
@RequiredArgsConstructor
public class ScmPurchaseOrderExtractTarget implements AiExtractTarget {

    private final ScmExtractSupport support;

    @Override
    public String key() {
        return "scm.purchase-order";
    }

    @Override
    public String labelKey() {
        return "scm.label.purchaseOrder";
    }

    @Override
    public String permission() {
        return "scm:purchase:add";
    }

    @Override
    public Map<String, Object> schema() {
        return support.orderSchema("supplierName", "scm.ai.extractSupplierName");
    }

    @Override
    public String instructionKey() {
        return "scm.ai.extractPurchaseInstruction";
    }

    @Override
    public AiPrefill prefill(Map<String, Object> extracted) {
        return support.orderPrefill(extracted, true);
    }

    @Override
    public String route() {
        return "/scm/purchase";
    }

    @Override
    public String form() {
        return "purchase-order";
    }
}
