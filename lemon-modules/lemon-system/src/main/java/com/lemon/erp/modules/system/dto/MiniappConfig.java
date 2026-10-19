package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 小程序公开配置
 *
 * @param subscribeTemplateIds 需要用户授权的订阅消息模板（审批提醒），没有配置时为空
 */
public record MiniappConfig(List<String> subscribeTemplateIds) {
}
