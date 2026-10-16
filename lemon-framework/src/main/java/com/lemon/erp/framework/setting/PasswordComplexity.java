package com.lemon.erp.framework.setting;

import java.util.regex.Pattern;

/** 密码复杂度 */
public enum PasswordComplexity implements OptionEnum {
    LETTER_DIGIT("字母和数字", "同时包含字母和数字", "(?=.*[A-Za-z])(?=.*\\d)"),
    MIXED_CASE_DIGIT("大小写字母和数字", "同时包含大写字母、小写字母和数字", "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)"),
    LETTER_DIGIT_SYMBOL("字母、数字和特殊字符", "同时包含字母、数字和特殊字符", "(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d])");

    private final String label;
    private final String requirement;
    private final Pattern pattern;

    PasswordComplexity(String label, String requirement, String lookahead) {
        this.label = label;
        this.requirement = requirement;
        this.pattern = Pattern.compile("^" + lookahead + ".*$");
    }

    @Override
    public String label() {
        return label;
    }

    /** 提示文字，如「同时包含字母和数字」 */
    public String requirement() {
        return requirement;
    }

    public boolean matches(String password) {
        return password != null && pattern.matcher(password).matches();
    }
}
