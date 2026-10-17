package com.lemon.erp.modules.system.im;

import com.lemon.erp.framework.message.MessageSender;

import java.util.List;

/** 站内消息已写入（事务提交后推送到钉钉、企业微信、飞书） */
public record MessageSentEvent(List<Long> userIds, MessageSender.Message message) {
}
