package com.lemon.erp.modules.ai.client;

import com.lemon.erp.common.i18n.Messages;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 精简的 JSON Schema 校验：校验模型生成的工具参数、JSON 输出。支持 type（可为数组）、properties、required、items、
 * enum、format（date）、minimum、maximum、minLength、maxLength、maxItems（与 {@code framework.ai.AiSchema} 一致），
 * 其他关键字忽略。值为 null 的非必填属性视为未填写。错误信息按当前语言生成，会回传给模型用于修正
 */
public final class JsonSchemaValidator {

    private static final String ROOT = "$";
    private static final String FORMAT_DATE = "date";

    private JsonSchemaValidator() {
    }

    /** @return 错误列表，校验通过时为空 */
    public static List<String> validate(JsonNode value, Map<String, Object> schema) {
        List<String> errors = new ArrayList<>();
        check(value, schema, ROOT, errors);
        return errors;
    }

    private static void check(JsonNode node, Map<String, Object> schema, String path, List<String> errors) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        Object type = schema.get("type");
        if (type != null && !matchesType(node, type)) {
            errors.add(Messages.get("ai.text.schemaType", path, type instanceof Collection<?> c ? String.join("|", c.stream().map(String::valueOf).toList()) : type));
            return;
        }
        if (schema.get("enum") instanceof Collection<?> allowed && !node.isNull()) {
            boolean ok = allowed.stream().anyMatch(a -> a != null && (node.isString() ? node.asString().equals(String.valueOf(a)) : node.toString().equals(String.valueOf(a))));
            if (!ok) {
                errors.add(Messages.get("ai.text.schemaEnum", path, allowed));
            }
        }
        if (node.isString()) {
            String s = node.asString();
            if (FORMAT_DATE.equals(schema.get("format")) && !isDate(s)) {
                errors.add(Messages.get("ai.text.schemaDate", path, s));
            }
            if (schema.get("minLength") instanceof Number min && s.length() < min.intValue()) {
                errors.add(Messages.get("ai.text.schemaMinLength", path, min));
            }
            if (schema.get("maxLength") instanceof Number max && s.length() > max.intValue()) {
                errors.add(Messages.get("ai.text.schemaMaxLength", path, max));
            }
        }
        if (node.isNumber()) {
            BigDecimal v = node.decimalValue();
            if (schema.get("minimum") instanceof Number min && v.compareTo(new BigDecimal(min.toString())) < 0) {
                errors.add(Messages.get("ai.text.schemaMinimum", path, min));
            }
            if (schema.get("maximum") instanceof Number max && v.compareTo(new BigDecimal(max.toString())) > 0) {
                errors.add(Messages.get("ai.text.schemaMaximum", path, max));
            }
        }
        if (node.isObject()) {
            if (schema.get("required") instanceof Collection<?> required) {
                for (Object name : required) {
                    JsonNode child = node.get(String.valueOf(name));
                    if (child == null || child.isNull()) {
                        errors.add(Messages.get("ai.text.schemaRequired", path + "." + name));
                    }
                }
            }
            if (schema.get("properties") instanceof Map<?, ?> props) {
                for (Map.Entry<?, ?> e : props.entrySet()) {
                    JsonNode child = node.get(String.valueOf(e.getKey()));
                    if (child != null && !child.isNull() && e.getValue() instanceof Map<?, ?> childSchema) {
                        check(child, cast(childSchema), path + "." + e.getKey(), errors);
                    }
                }
            }
        }
        if (node.isArray()) {
            if (schema.get("maxItems") instanceof Number max && node.size() > max.intValue()) {
                errors.add(Messages.get("ai.text.schemaMaxItems", path, max));
            }
            if (schema.get("items") instanceof Map<?, ?> items) {
                for (int i = 0; i < node.size(); i++) {
                    check(node.get(i), cast(items), path + "[" + i + "]", errors);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static boolean matchesType(JsonNode node, Object type) {
        if (type instanceof Collection<?> types) {
            return types.stream().anyMatch(t -> matchesType(node, t));
        }
        return switch (String.valueOf(type)) {
            case "object" -> node.isObject();
            case "array" -> node.isArray();
            case "string" -> node.isString();
            case "integer" -> node.isIntegralNumber() || node.isNumber() && node.decimalValue().stripTrailingZeros().scale() <= 0;
            case "number" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "null" -> node.isNull();
            default -> true;
        };
    }

    private static boolean isDate(String s) {
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
