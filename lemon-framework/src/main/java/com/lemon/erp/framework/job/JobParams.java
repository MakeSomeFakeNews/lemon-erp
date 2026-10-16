package com.lemon.erp.framework.job;

import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务参数：key=value，多个用逗号或分号分隔，如 days=30,notify=true
 */
public final class JobParams {

    private final Map<String, String> values = new HashMap<>();

    public JobParams(String text) {
        if (text == null) {
            return;
        }
        for (String pair : text.split("[,;，；]")) {
            int i = pair.indexOf('=');
            if (i > 0) {
                values.put(pair.substring(0, i).trim(), pair.substring(i + 1).trim());
            }
        }
    }

    public int getInt(String key, int defaultValue) {
        try {
            return values.containsKey(key) ? Integer.parseInt(values.get(key)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String get(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }
}
