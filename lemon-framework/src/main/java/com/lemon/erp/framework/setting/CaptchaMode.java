package com.lemon.erp.framework.setting;

/** 登录验证码 */
public enum CaptchaMode implements OptionEnum {
    OFF("framework.enum.CaptchaMode.off"),
    ALWAYS("framework.enum.CaptchaMode.always"),
    AFTER_FAILURE("framework.enum.CaptchaMode.afterFailure");

    private final String label;

    CaptchaMode(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
