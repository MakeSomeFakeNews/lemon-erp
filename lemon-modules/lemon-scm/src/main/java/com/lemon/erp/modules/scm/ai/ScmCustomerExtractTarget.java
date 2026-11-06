package com.lemon.erp.modules.scm.ai;

import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 智能录入：客户资料（名片、营业执照、开票信息），提示已有相同税号或名称的客户 */
@Component
@Order(40) // 目标列表中的顺序
@RequiredArgsConstructor
public class ScmCustomerExtractTarget implements AiExtractTarget {

    private final ScmExtractSupport support;

    @Override
    public String key() {
        return "scm.customer";
    }

    @Override
    public String labelKey() {
        return "scm.label.customer";
    }

    @Override
    public String permission() {
        return "scm:customer:add";
    }

    @Override
    public Map<String, Object> schema() {
        return support.partnerSchema(false);
    }

    @Override
    public String instructionKey() {
        return "scm.ai.extractPartnerInstruction";
    }

    @Override
    public AiPrefill prefill(Map<String, Object> extracted) {
        return support.partnerPrefill(extracted, false);
    }

    @Override
    public String route() {
        return "/scm/customer";
    }

    @Override
    public String form() {
        return "customer";
    }
}
