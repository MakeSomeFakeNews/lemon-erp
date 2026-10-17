package com.lemon.erp.framework.setting;

/**
 * 系统参数：参数设置页面按分组列出，修改后保存在 sys_config（平台级，所有租户共用），未修改的使用默认值。
 * 默认值优先取配置文件中的属性（property），没有配置时取 defaultValue。
 * 名称、说明、分组名称都是消息键（framework 资源包），输出时 {@code Messages.get}
 */
public enum Setting {

    SYSTEM_NAME("sys.name", Group.BASIC, "framework.label.setting.systemName", SettingType.TEXT, null, "柠檬ERP", null, 1, 30, // i18n-ignore 默认值，未修改时登录页按语言显示 framework.label.defaultSystemName
            "framework.label.setting.systemName.remark"),
    SHOW_DEMO_ACCOUNT("sys.showDemoAccount", Group.BASIC, "framework.label.setting.showDemoAccount", SettingType.BOOL, null, "true", null, 0, 0,
            "framework.label.setting.showDemoAccount.remark"),
    WATERMARK("sys.watermark", Group.BASIC, "framework.label.setting.watermark", SettingType.BOOL, null, "false", null, 0, 0,
            "framework.label.setting.watermark.remark"),

    CAPTCHA_MODE("captcha.mode", Group.LOGIN, "framework.label.setting.captchaMode", SettingType.SELECT, null, CaptchaMode.OFF.name(), CaptchaMode.class, 0, 0,
            "framework.label.setting.captchaMode.remark"),
    CAPTCHA_TYPE("captcha.type", Group.LOGIN, "framework.label.setting.captchaType", SettingType.SELECT, null, CaptchaType.MATH.name(), CaptchaType.class, 0, 0,
            "framework.label.setting.captchaType.remark"),
    LOGIN_MAX_RETRY("login.maxRetry", Group.LOGIN, "framework.label.setting.loginMaxRetry", SettingType.NUMBER, "lemon.security.login-max-retry", "5", null, 1, 20,
            "framework.label.setting.loginMaxRetry.remark"),
    LOGIN_LOCK_MINUTES("login.lockMinutes", Group.LOGIN, "framework.label.setting.loginLockMinutes", SettingType.NUMBER, "lemon.security.login-lock-minutes", "10", null, 1, 1440,
            null),
    SESSION_EXPIRE_HOURS("session.expireHours", Group.LOGIN, "framework.label.setting.sessionExpireHours", SettingType.NUMBER, "lemon.jwt.expire-hours", "12", null, 1, 720,
            "framework.label.setting.sessionExpireHours.remark"),
    SESSION_SINGLE("session.single", Group.LOGIN, "framework.label.setting.sessionSingle", SettingType.BOOL, null, "false", null, 0, 0,
            "framework.label.setting.sessionSingle.remark"),

    PASSWORD_MIN_LENGTH("password.minLength", Group.PASSWORD, "framework.label.setting.passwordMinLength", SettingType.NUMBER, "lemon.security.password-min-length", "8", null, 6, 32,
            "framework.label.setting.passwordMinLength.remark"),
    PASSWORD_COMPLEXITY("password.complexity", Group.PASSWORD, "framework.label.setting.passwordComplexity", SettingType.SELECT, null, PasswordComplexity.LETTER_DIGIT.name(), PasswordComplexity.class, 0, 0,
            "framework.label.setting.passwordComplexity.remark"),
    PASSWORD_EXPIRE_DAYS("password.expireDays", Group.PASSWORD, "framework.label.setting.passwordExpireDays", SettingType.NUMBER, "lemon.security.password-expire-days", "0", null, 0, 3650,
            "framework.label.setting.passwordExpireDays.remark"),
    USER_INITIAL_PASSWORD("user.initialPassword", Group.PASSWORD, "framework.label.setting.userInitialPassword", SettingType.TEXT, "lemon.user.initial-password", "Lemon@123", null, 6, 32,
            "framework.label.setting.userInitialPassword.remark"),

    ATTACHMENT_MAX_MB("upload.attachmentMaxMb", Group.UPLOAD, "framework.label.setting.attachmentMaxMb", SettingType.NUMBER, null, "20", null, 1, 100,
            "framework.label.setting.attachmentMaxMb.remark");

    /** 参数分组，按声明顺序显示 */
    public enum Group {
        BASIC("framework.enum.SettingGroup.basic"),
        LOGIN("framework.enum.SettingGroup.login"),
        PASSWORD("framework.enum.SettingGroup.password"),
        UPLOAD("framework.enum.SettingGroup.upload");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final String key;
    private final Group group;
    private final String label;
    private final SettingType type;
    private final String property;
    private final String defaultValue;
    private final Class<? extends OptionEnum> options;
    private final int min;
    private final int max;
    private final String remark;

    Setting(String key, Group group, String label, SettingType type, String property, String defaultValue,
            Class<? extends OptionEnum> options, int min, int max, String remark) {
        this.key = key;
        this.group = group;
        this.label = label;
        this.type = type;
        this.property = property;
        this.defaultValue = defaultValue;
        this.options = options;
        this.min = min;
        this.max = max;
        this.remark = remark;
    }

    public String key() {
        return key;
    }

    public Group group() {
        return group;
    }

    public String label() {
        return label;
    }

    public SettingType type() {
        return type;
    }

    /** 提供默认值的配置文件属性，为空时没有 */
    public String property() {
        return property;
    }

    public String defaultValue() {
        return defaultValue;
    }

    /** SELECT 的取值枚举 */
    public Class<? extends OptionEnum> options() {
        return options;
    }

    /** NUMBER 的取值范围；TEXT 的长度范围 */
    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public String remark() {
        return remark;
    }

    public static Setting ofKey(String key) {
        for (Setting s : values()) {
            if (s.key.equals(key)) {
                return s;
            }
        }
        return null;
    }
}
