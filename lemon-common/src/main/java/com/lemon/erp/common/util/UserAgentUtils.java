package com.lemon.erp.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易 User-Agent 解析，只识别常见浏览器和操作系统
 */
public final class UserAgentUtils {

    private static final String UNKNOWN = "未知";

    private record Browser(String name, Pattern pattern) {
    }

    /** 顺序有意义：Edge/Opera 的 UA 中也包含 Chrome，Chrome 的 UA 中也包含 Safari */
    private static final Browser[] BROWSERS = {
            new Browser("Edge", Pattern.compile("Edg(?:e|A|iOS)?/([\\d.]+)")),
            new Browser("Opera", Pattern.compile("(?:OPR|Opera)/([\\d.]+)")),
            new Browser("WeChat", Pattern.compile("MicroMessenger/([\\d.]+)")),
            new Browser("Firefox", Pattern.compile("Firefox/([\\d.]+)")),
            new Browser("Chrome", Pattern.compile("(?:Chrome|CriOS)/([\\d.]+)")),
            new Browser("Safari", Pattern.compile("Version/([\\d.]+).*Safari")),
            new Browser("IE", Pattern.compile("(?:MSIE |Trident/.*rv:)([\\d.]+)")),
    };

    private UserAgentUtils() {
    }

    public static String getBrowser(String ua) {
        if (ua == null || ua.isBlank()) {
            return UNKNOWN;
        }
        for (Browser b : BROWSERS) {
            Matcher m = b.pattern().matcher(ua);
            if (m.find()) {
                String version = m.group(1);
                int dot = version.indexOf('.');
                return b.name() + " " + (dot > 0 ? version.substring(0, dot) : version);
            }
        }
        if (ua.contains("curl") || ua.contains("PostmanRuntime") || ua.contains("okhttp") || ua.contains("Java")) {
            return ua.split("[/ ]")[0];
        }
        return UNKNOWN;
    }

    public static String getOs(String ua) {
        if (ua == null || ua.isBlank()) {
            return UNKNOWN;
        }
        if (ua.contains("Windows NT 10")) return "Windows 10/11";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("HarmonyOS")) return "HarmonyOS";
        if (ua.contains("Linux")) return "Linux";
        return UNKNOWN;
    }
}
