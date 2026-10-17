package com.lemon.erp.modules.system.im;

/** 第三方集成的平台（字典 sys_im_platform） */
public enum ImPlatform {
    DINGTALK("钉钉"),
    WECOM("企业微信"),
    FEISHU("飞书");

    private final String label;

    ImPlatform(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
