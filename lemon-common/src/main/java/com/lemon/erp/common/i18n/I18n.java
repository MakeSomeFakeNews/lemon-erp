package com.lemon.erp.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 后端消息的多语言。代码中直接写中文（默认语言，同时作为翻译键），按请求头 {@value #HEADER} 返回对应语言：
 * <ul>
 *   <li>译文在各模块的 {@code resources/i18n/<模块>.<语言>.json}（如 {@code system.en-US.json}），内容为「中文模板 → 译文」，
 *       启动后首次使用时合并 {@code classpath*:i18n/*.json}</li>
 *   <li>模板中的参数写作 {0} {1}（不用 MessageFormat，避免单引号转义和数字千分位），{@link #format} 按位置替换</li>
 *   <li>没有请求头、不在请求中（定时任务、异步线程）或没有译文时返回中文；测试默认不带请求头，断言中文消息不受影响</li>
 * </ul>
 * 业务异常 {@code new BizException("登录名「{0}」已存在", username)} 由全局异常处理按请求语言翻译；
 * 返回给前端的其他固定文字（枚举名称、报表行名、待办标题等）在输出处调用 {@link #t}
 */
@Slf4j
public final class I18n {

    public static final String HEADER = "X-Lang";
    public static final String DEFAULT = "zh-CN";
    /** 支持的语言（除默认语言外需要有译文文件） */
    public static final List<String> LANGS = List.of(DEFAULT, "en-US");

    private static volatile Map<String, Map<String, String>> bundles;

    private I18n() {
    }

    /** 当前请求的语言，不在请求中或不支持时为默认语言 */
    public static String lang() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return normalize(sra.getRequest().getHeader(HEADER));
        }
        return DEFAULT;
    }

    public static String lang(HttpServletRequest request) {
        return normalize(request.getHeader(HEADER));
    }

    /** 把 en、en_US、en-us 等归一为支持的语言，其他返回默认语言 */
    public static String normalize(String tag) {
        if (tag == null || tag.isBlank()) {
            return DEFAULT;
        }
        String t = tag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        for (String lang : LANGS) {
            String l = lang.toLowerCase(Locale.ROOT);
            if (t.equals(l) || t.equals(l.substring(0, l.indexOf('-')))) {
                return lang;
            }
        }
        return DEFAULT;
    }

    public static boolean isDefault() {
        return DEFAULT.equals(lang());
    }

    /** 按当前请求的语言翻译中文模板并填入参数 */
    public static String t(String template, Object... args) {
        return translate(lang(), template, args);
    }

    /** 按指定语言翻译（与 t(模板, 参数…) 分开命名，避免只有一个字符串参数时重载歧义） */
    public static String translate(String lang, String template, Object... args) {
        if (template == null) {
            return null;
        }
        String text = template;
        if (!DEFAULT.equals(lang)) {
            String translated = bundles().getOrDefault(lang, Map.of()).get(template);
            if (translated != null) {
                text = translated;
            }
        }
        return format(text, args);
    }

    /** 是否有译文（检查遗漏的翻译） */
    public static boolean has(String lang, String template) {
        return DEFAULT.equals(lang) || bundles().getOrDefault(lang, Map.of()).containsKey(template);
    }

    /** 已登记译文的中文模板 */
    public static Set<String> keys(String lang) {
        return bundles().getOrDefault(lang, Map.of()).keySet();
    }

    /** 按位置替换 {0} {1}…；参数为 null 时替换为空 */
    public static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 16);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i);
                if (end > i + 1) {
                    String idx = template.substring(i + 1, end);
                    if (idx.chars().allMatch(Character::isDigit) && idx.length() <= 2) {
                        int n = Integer.parseInt(idx);
                        if (n < args.length) {
                            sb.append(args[n] == null ? "" : args[n]);
                            i = end + 1;
                            continue;
                        }
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static Map<String, Map<String, String>> bundles() {
        Map<String, Map<String, String>> b = bundles;
        if (b == null) {
            synchronized (I18n.class) {
                if (bundles == null) {
                    bundles = load();
                }
                b = bundles;
            }
        }
        return b;
    }

    private static Map<String, Map<String, String>> load() {
        Map<String, Map<String, String>> result = new HashMap<>();
        JsonMapper mapper = JsonMapper.builder().build();
        try {
            for (Resource r : new PathMatchingResourcePatternResolver(I18n.class.getClassLoader()).getResources("classpath*:i18n/*.json")) {
                String name = r.getFilename();
                // <模块>.<语言>.json
                String[] parts = name == null ? new String[0] : name.split("\\.");
                if (parts.length != 3) {
                    continue;
                }
                try (InputStream in = r.getInputStream()) {
                    Map<String, String> map = mapper.readValue(in, new TypeReference<Map<String, String>>() {
                    });
                    result.computeIfAbsent(normalize(parts[1]), k -> new HashMap<>()).putAll(map);
                }
            }
        } catch (IOException e) {
            log.warn("加载多语言文件失败", e);
        }
        result.remove(DEFAULT);
        return result;
    }
}
