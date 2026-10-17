package com.lemon.erp.framework.setting;

import java.util.regex.Pattern;

/** 密码复杂度 */
public enum PasswordComplexity implements OptionEnum {
    LETTER_DIGIT("framework.enum.PasswordComplexity.letterDigit", "framework.enum.PasswordComplexity.letterDigit.requirement", "(?=.*[A-Za-z])(?=.*\\d)"),
    MIXED_CASE_DIGIT("framework.enum.PasswordComplexity.mixedCaseDigit", "framework.enum.PasswordComplexity.mixedCaseDigit.requirement", "(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)"),
    LETTER_DIGIT_SYMBOL("framework.enum.PasswordComplexity.letterDigitSymbol", "framework.enum.PasswordComplexity.letterDigitSymbol.requirement", "(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d])");

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

    /** 提示文字的消息键，如「同时包含字母和数字」 */
    public String requirement() {
        return requirement;
    }

    public boolean matches(String password) {
        return password != null && pattern.matcher(password).matches();
    }
}
