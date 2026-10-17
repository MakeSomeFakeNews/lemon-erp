package com.lemon.erp.modules.system.im;

/**
 * 推送到平台的消息卡片
 *
 * @param url 点击后打开的地址（绝对地址），为空时只发送文字
 */
public record ImCard(String title, String content, String url) {
}
