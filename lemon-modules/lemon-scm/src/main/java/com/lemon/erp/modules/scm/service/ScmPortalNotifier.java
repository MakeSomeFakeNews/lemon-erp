package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.portal.PortalAccounts;
import com.lemon.erp.framework.portal.PortalKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 给门户账号发送站内消息：供应商（订单待确认、招标邀请、中标通知、对账单待确认等）、经销商（客户对账单待确认）
 */
@Component
@RequiredArgsConstructor
public class ScmPortalNotifier {

    private final PortalAccounts portalAccounts;
    private final MessageSender messageSender;

    /**
     * @param link 门户页面路由，如 /portal/order
     */
    public void notify(Long supplierId, MessageCategory category, String title, String content, String link) {
        notify(PortalKind.SUPPLIER, supplierId, category, title, content, link);
    }

    /**
     * 给供应商门户 / 经销商门户的账号发消息
     *
     * @param partnerId 供应商 ID / 客户 ID
     * @param link      门户页面路由，如 /portal/order、/dealer/statement
     */
    public void notify(PortalKind kind, Long partnerId, MessageCategory category, String title, String content, String link) {
        List<Long> users = portalAccounts.enabledUserIds(kind, partnerId);
        if (!users.isEmpty()) {
            messageSender.send(users, MessageSender.Message.of(category, title, content, link));
        }
    }
}
