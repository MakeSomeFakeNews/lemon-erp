package com.lemon.erp.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * 国际化消息（Spring {@link MessageSource} + 资源包）：
 * <ul>
 *   <li>每个模块的消息在 {@code src/main/resources/i18n/<模块>.properties}（默认，简体中文）和 {@code <模块>_en.properties}（英文），
 *       键为 {@code <模块>.<分类>.<名称>}，参数按 MessageFormat 写作 {0}（文字中的单引号写成两个）；启动时自动发现 classpath 下全部资源包</li>
 *   <li>语言由请求头 Accept-Language 决定（{@code I18nConfig} 中的 LocaleResolver），没有请求头、不在请求中（定时任务、异步线程）时为简体中文</li>
 *   <li>同一实例也注册为 Spring 的 messageSource，校验注解写 {@code message = "{键}"}</li>
 *   <li>参数一律按 {@code String.valueOf} 转为文字后填入，数字不按语言加千分位，与原来的提示保持一致</li>
 * </ul>
 */
public final class Messages {

    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;
    /**
     * 支持的语言：AcceptHeaderLocaleResolver 先精确匹配，再按语言匹配不带国家的项，
     * 所以 en-US、en-GB、en 都对应 en（英文资源包），zh-TW 等对应 zh（默认资源包）
     */
    public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.ENGLISH);
    private static final String LOCATION = "classpath*:i18n/*.properties";

    private static volatile ResourceBundleMessageSource source;

    private Messages() {
    }

    /** 全部模块的资源包：i18n 目录下不带语言后缀的 properties 文件各是一个资源包 */
    public static MessageSource messageSource() {
        ResourceBundleMessageSource s = source;
        if (s == null) {
            synchronized (Messages.class) {
                if (source == null) {
                    source = create();
                }
                s = source;
            }
        }
        return s;
    }

    private static ResourceBundleMessageSource create() {
        TreeSet<String> basenames = new TreeSet<>();
        try {
            for (Resource r : new PathMatchingResourcePatternResolver(Messages.class.getClassLoader()).getResources(LOCATION)) {
                String name = r.getFilename();
                if (name != null && !name.contains("_")) {
                    basenames.add("i18n/" + name.substring(0, name.length() - ".properties".length()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ResourceBundleMessageSource s = new ResourceBundleMessageSource();
        s.setBasenames(basenames.toArray(String[]::new));
        s.setDefaultEncoding("UTF-8");
        // 没有对应语言的资源包时用默认资源包（中文），不按服务器的系统语言
        s.setFallbackToSystemLocale(false);
        s.setAlwaysUseMessageFormat(true);
        // 键不存在时返回键本身，便于发现遗漏
        s.setUseCodeAsDefaultMessage(true);
        return s;
    }

    /** 当前请求的语言；不在请求中时为默认语言（不用服务器的系统语言） */
    public static Locale locale() {
        LocaleContext ctx = LocaleContextHolder.getLocaleContext();
        Locale locale = ctx == null ? null : ctx.getLocale();
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    public static boolean isDefaultLanguage() {
        return DEFAULT_LOCALE.getLanguage().equals(locale().getLanguage());
    }

    /** 按当前请求的语言取消息 */
    public static String get(String code, Object... args) {
        return get(locale(), code, args);
    }

    public static String get(Locale locale, String code, Object... args) {
        if (code == null) {
            return null;
        }
        return messageSource().getMessage(code, stringify(args), code, locale);
    }

    /** 键是否存在（不回退为键本身） */
    public static boolean exists(Locale locale, String code) {
        return messageSource().getMessage(code, null, null, locale) != null;
    }

    private static Object[] stringify(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = args[i] == null ? "" : String.valueOf(args[i]);
        }
        return out;
    }
}
