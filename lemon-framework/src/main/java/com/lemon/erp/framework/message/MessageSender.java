package com.lemon.erp.framework.message;

import java.util.Collection;

/**
 * 站内消息：业务模块发送审批待办、到期提醒、预警等通知，由系统模块实现（{@code SysMessageService}）。
 * 在业务事务中调用，事务回滚时消息一并回滚
 */
public interface MessageSender {

    /** 发送给指定用户（忽略空 ID、重复 ID） */
    void send(Collection<Long> userIds, Message message);

    /**
     * 发送给拥有某个权限标识的全部启用用户；没有任何用户拥有该权限时发给超级管理员
     *
     * @param perm 权限标识，如 hr:employee:list
     */
    void sendToPermission(String perm, Message message);

    /**
     * @param category 字典 sys_message_category：APPROVAL 审批、REMIND 提醒、ALERT 预警、SYSTEM 系统
     * @param link     前端路由（如 /workflow/todo），点击消息跳转
     * @param dedupKey 去重键：同一用户已有相同去重键的消息时不再发送（定时提醒每天只发一次），为空不去重
     */
    record Message(String category, String title, String content, String link, String dedupKey) {

        public static Message of(String category, String title, String content, String link) {
            return new Message(category, title, content, link, null);
        }

        public Message dedup(String key) {
            return new Message(category, title, content, link, key);
        }
    }
}
