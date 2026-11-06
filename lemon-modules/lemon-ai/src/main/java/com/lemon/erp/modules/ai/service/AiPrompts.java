package com.lemon.erp.modules.ai.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ai.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.TextStyle;

/** 给模型的系统提示（消息键在 ai.properties 中，按当前用户语言生成） */
@Component
@RequiredArgsConstructor
public class AiPrompts {

    private final AiConfigMapper configMapper;
    private final UserLookup userLookup;

    /**
     * AI 问数的系统提示：身份、只能基于工具结果回答、不编造数字、金额带单位、今天日期、当前公司和用户、回答语言
     *
     * @param hasTools 当前用户是否有可用的查询工具
     */
    public String chatSystem(boolean hasTools) {
        LocalDate today = LocalDate.now();
        String weekday = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Messages.locale());
        String tenant = configMapper.selectTenantName(TenantContext.currentOrPlatform());
        LoginUser user = SecurityUtils.getLoginUser();
        String name = userLookup.nameOf(user.userId());
        String prompt = Messages.get("ai.prompt.chatSystem", today, weekday, tenant == null ? "" : tenant,
                name == null ? user.username() : name, Messages.get("ai.prompt.language"));
        return prompt + "\n" + Messages.get(hasTools ? "ai.prompt.chatTools" : "ai.prompt.chatNoTools");
    }
}
