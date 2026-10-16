package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.portal.PortalAccounts;
import com.lemon.erp.framework.portal.PortalKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 给供应商门户账号发送站内消息（订单待确认、招标邀请、中标通知、对账单待确认等）
 */
@Component
@RequiredArgsConstructor
public class ScmPortalNotifier {

    private final PortalAccounts portalAccounts;
    private final MessageSender messageSender;

    /**
     * @param link 门户页面路由，如 /portal/order
     */
    public void notify(Long supplierId, String category, String title, String content, String link) {
        List<Long> users = portalAccounts.enabledUserIds(PortalKind.SUPPLIER, supplierId);
        if (!users.isEmpty()) {
            messageSender.send(users, MessageSender.Message.of(category, title, content, link));
        }
    }
}
