package com.lemon.erp.framework.ai;

import com.lemon.erp.common.i18n.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 构造 JSON Schema（AI 工具的参数、智能录入的输出结构）的小工具，结果是可直接序列化为 JSON 的 {@code Map}。
 * <p>
 * 说明文字是给模型看的提示词，传<b>消息键</b>，在 {@link #build()} / 各工厂方法调用时按当前用户语言取文字，
 * 所以 {@code AiTool#parameters()} 等方法应在每次调用时重新构造，不要缓存在静态字段里。
 * 支持的关键字与 AI 模块的校验器一致：type、properties、required、items、enum、format（date）、minimum、maximum、
 * minLength、maxLength、maxItems、description。
 *
 * <pre>{@code
 * AiSchema.object()
 *         .prop("startDate", AiSchema.date("scm.ai.param.startDate"), true)
 *         .prop("customer", AiSchema.string("scm.ai.param.customer"), false)
 *         .prop("top", AiSchema.integer("scm.ai.param.top").min(1).max(200), false)
 *         .build();
 * }</pre>
 */
public final class AiSchema {

    private static final String OBJECT = "object";

    private final Map<String, Object> schema = new LinkedHashMap<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();
    private final boolean objectType;

    private AiSchema(String type, String descriptionKey) {
        schema.put("type", type);
        objectType = OBJECT.equals(type);
        if (descriptionKey != null) {
            schema.put("description", Messages.get(descriptionKey));
        }
    }

    /** 对象，用 {@link #prop} 添加属性 */
    public static AiSchema object() {
        return new AiSchema(OBJECT, null);
    }

    public static AiSchema object(String descriptionKey) {
        return new AiSchema(OBJECT, descriptionKey);
    }

    public static AiSchema string(String descriptionKey) {
        return new AiSchema("string", descriptionKey);
    }

    /** 日期字符串 yyyy-MM-dd */
    public static AiSchema date(String descriptionKey) {
        AiSchema s = new AiSchema("string", descriptionKey);
        s.schema.put("format", "date");
        return s;
    }

    public static AiSchema integer(String descriptionKey) {
        return new AiSchema("integer", descriptionKey);
    }

    public static AiSchema number(String descriptionKey) {
        return new AiSchema("number", descriptionKey);
    }

    public static AiSchema bool(String descriptionKey) {
        return new AiSchema("boolean", descriptionKey);
    }

    /** 只能取给定值的字符串（如枚举名称） */
    public static AiSchema enumOf(String descriptionKey, String... values) {
        AiSchema s = new AiSchema("string", descriptionKey);
        s.schema.put("enum", Arrays.asList(values));
        return s;
    }

    public static AiSchema array(String descriptionKey, AiSchema items) {
        AiSchema s = new AiSchema("array", descriptionKey);
        s.schema.put("items", items.build());
        return s;
    }

    /** 添加对象属性 */
    public AiSchema prop(String name, AiSchema value, boolean isRequired) {
        properties.put(name, value.build());
        if (isRequired) {
            required.add(name);
        }
        return this;
    }

    public AiSchema min(Number min) {
        schema.put("minimum", min);
        return this;
    }

    public AiSchema max(Number max) {
        schema.put("maximum", max);
        return this;
    }

    public AiSchema maxLength(int max) {
        schema.put("maxLength", max);
        return this;
    }

    public AiSchema maxItems(int max) {
        schema.put("maxItems", max);
        return this;
    }

    /** 生成 JSON Schema（每次返回新的 Map） */
    public Map<String, Object> build() {
        Map<String, Object> out = new LinkedHashMap<>(schema);
        if (objectType) {
            out.put("properties", new LinkedHashMap<>(properties));
            if (!required.isEmpty()) {
                out.put("required", List.copyOf(required));
            }
        }
        return out;
    }
}
