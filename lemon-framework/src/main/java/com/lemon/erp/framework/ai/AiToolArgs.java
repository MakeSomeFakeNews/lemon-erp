package com.lemon.erp.framework.ai;

import com.lemon.erp.common.exception.BizException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型调用工具时传入的参数（已按 {@link AiTool#parameters()} 的 JSON Schema 校验过）。
 * 取值方法在参数不存在或为 null 时返回 null（或给定的默认值），类型不对时抛出 {@link BizException}
 * （错误信息会作为工具结果回传给模型，模型可据此修正参数重试）。
 */
public final class AiToolArgs {

    private final Map<String, Object> values;

    public AiToolArgs(Map<String, Object> values) {
        this.values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** 全部参数（只读），值为 JSON 解析后的 String / Number / Boolean / List / Map */
    public Map<String, Object> raw() {
        return values;
    }

    public boolean has(String name) {
        return values.get(name) != null;
    }

    public String getString(String name) {
        Object v = values.get(name);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    public Long getLong(String name) {
        Object v = values.get(name);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw invalid(name, v);
        }
    }

    public int getInt(String name, int defaultValue) {
        Long v = getLong(name);
        return v == null ? defaultValue : Math.toIntExact(v);
    }

    public BigDecimal getDecimal(String name) {
        Object v = values.get(name);
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw invalid(name, v);
        }
    }

    /** yyyy-MM-dd */
    public LocalDate getDate(String name) {
        String s = getString(name);
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw invalid(name, s);
        }
    }

    public Boolean getBoolean(String name) {
        Object v = values.get(name);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    /** 字符串数组 */
    public List<String> getStrings(String name) {
        Object v = values.get(name);
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> list) {
            return list.stream().filter(java.util.Objects::nonNull).map(String::valueOf).toList();
        }
        throw invalid(name, v);
    }

    private static BizException invalid(String name, Object value) {
        return new BizException("framework.error.aiInvalidToolArgument", name, value);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
