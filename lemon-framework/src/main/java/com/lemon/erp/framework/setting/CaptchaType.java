package com.lemon.erp.framework.setting;

/** 验证码类型 */
public enum CaptchaType implements OptionEnum {
    MATH("framework.enum.CaptchaType.math"),
    CHAR("framework.enum.CaptchaType.char");

    private final String label;

    CaptchaType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
