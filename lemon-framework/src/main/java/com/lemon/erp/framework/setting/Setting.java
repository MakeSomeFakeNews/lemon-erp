package com.lemon.erp.framework.setting;

/**
 * 系统参数：参数设置页面按分组列出，修改后保存在 sys_config（平台级，所有租户共用），未修改的使用默认值。
 * 默认值优先取配置文件中的属性（property），没有配置时取 defaultValue
 */
public enum Setting {

    SYSTEM_NAME("sys.name", Group.BASIC, "系统名称", SettingType.TEXT, null, "柠檬ERP", null, 1, 30,
            "显示在登录页、侧边栏和浏览器标题中"),
    SHOW_DEMO_ACCOUNT("sys.showDemoAccount", Group.BASIC, "登录页显示演示账号", SettingType.BOOL, null, "true", null, 0, 0,
            "正式使用时请关闭"),
    WATERMARK("sys.watermark", Group.BASIC, "页面水印", SettingType.BOOL, null, "false", null, 0, 0,
            "在页面上显示当前用户的姓名和账号，防止截图外传"),

    CAPTCHA_MODE("captcha.mode", Group.LOGIN, "登录验证码", SettingType.SELECT, null, "OFF", CaptchaMode.class, 0, 0,
            "「密码输错后」：同一账号密码输错后，下次登录需要输入验证码"),
    CAPTCHA_TYPE("captcha.type", Group.LOGIN, "验证码类型", SettingType.SELECT, null, "MATH", CaptchaType.class, 0, 0,
            "算术题（如 3 + 5 = ?）或 4 位字母数字，2 分钟内有效"),
    LOGIN_MAX_RETRY("login.maxRetry", Group.LOGIN, "密码错误锁定次数", SettingType.NUMBER, "lemon.security.login-max-retry", "5", null, 1, 20,
            "连续输错达到次数后锁定账号，锁定期间密码正确也不能登录；管理员重置密码后解锁"),
    LOGIN_LOCK_MINUTES("login.lockMinutes", Group.LOGIN, "锁定时长（分钟）", SettingType.NUMBER, "lemon.security.login-lock-minutes", "10", null, 1, 1440,
            null),
    SESSION_EXPIRE_HOURS("session.expireHours", Group.LOGIN, "登录有效期（小时）", SettingType.NUMBER, "lemon.jwt.expire-hours", "12", null, 1, 720,
            "登录后超过有效期需要重新登录，修改后对新登录生效"),
    SESSION_SINGLE("session.single", Group.LOGIN, "同一账号只允许一处登录", SettingType.BOOL, null, "false", null, 0, 0,
            "开启后，账号在别处登录时，之前的登录自动下线"),

    PASSWORD_MIN_LENGTH("password.minLength", Group.PASSWORD, "密码最小长度", SettingType.NUMBER, "lemon.security.password-min-length", "8", null, 6, 32,
            "最长 32 位"),
    PASSWORD_COMPLEXITY("password.complexity", Group.PASSWORD, "密码复杂度", SettingType.SELECT, null, "LETTER_DIGIT", PasswordComplexity.class, 0, 0,
            "对新设置的密码生效（新增用户、重置、修改密码）"),
    PASSWORD_EXPIRE_DAYS("password.expireDays", Group.PASSWORD, "密码有效期（天）", SettingType.NUMBER, "lemon.security.password-expire-days", "0", null, 0, 3650,
            "超过有效期登录后要求修改密码，0 不限制"),
    USER_INITIAL_PASSWORD("user.initialPassword", Group.PASSWORD, "初始密码", SettingType.TEXT, "lemon.user.initial-password", "Lemon@123", null, 6, 32,
            "人事录用、组织同步自动开通账号时的初始密码，须符合密码策略；首次登录要求修改"),

    ATTACHMENT_MAX_MB("upload.attachmentMaxMb", Group.UPLOAD, "附件大小上限（MB）", SettingType.NUMBER, null, "20", null, 1, 100,
            "单据附件单个文件的大小上限");

    /** 参数分组，按声明顺序显示 */
    public enum Group {
        BASIC("基本设置"),
        LOGIN("登录与会话"),
        PASSWORD("密码策略"),
        UPLOAD("文件上传");

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
