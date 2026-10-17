package com.lemon.erp.modules.system.im;

import com.lemon.erp.common.i18n.Messages;

/** 第三方集成的平台（字典 sys_im_platform） */
public enum ImPlatform {
    DINGTALK("system.label.dingtalk", "system.label.dingtalkLogin"),
    WECOM("system.label.wecom", "system.label.wecomLogin"),
    FEISHU("system.label.feishu", "system.label.feishuLogin");

    private final String label;
    private final String loginLabel;

    ImPlatform(String label, String loginLabel) {
        this.label = label;
        this.loginLabel = loginLabel;
    }

    /** 名称的消息键 */
    public String label() {
        return label;
    }

    /** 登录方式（如「钉钉登录」）的消息键 */
    public String loginLabel() {
        return loginLabel;
    }

    /** 当前请求语言的名称 */
    public String text() {
        return Messages.get(label);
    }

    /** 默认语言（中文）的名称：写入日志、登录日志等存库内容 */
    public String defaultText() {
        return Messages.get(Messages.DEFAULT_LOCALE, label);
    }
}
