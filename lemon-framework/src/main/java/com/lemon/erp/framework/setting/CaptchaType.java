package com.lemon.erp.framework.setting;

/** 验证码类型 */
public enum CaptchaType implements OptionEnum {
    MATH("算术题"),
    CHAR("字母数字");

    private final String label;

    CaptchaType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
