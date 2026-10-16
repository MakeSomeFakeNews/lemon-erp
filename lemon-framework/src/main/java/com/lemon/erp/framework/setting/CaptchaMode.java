package com.lemon.erp.framework.setting;

/** 登录验证码 */
public enum CaptchaMode implements OptionEnum {
    OFF("关闭"),
    ALWAYS("每次登录"),
    AFTER_FAILURE("密码输错后");

    private final String label;

    CaptchaMode(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
