package com.lemon.erp.framework.ai;

import com.lemon.erp.common.exception.BizException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link AiCleanseSource} 实现的公共方法：应用建议前按实体上的校验注解校验（与接口的 {@code @Validated} 相同的规则，
 * 业务 Service 的修改方法本身不做 Bean Validation），构造候选记录的 extra
 */
public final class AiCleanseSupport {

    private AiCleanseSupport() {
    }

    /**
     * 按校验注解校验实体，不通过时抛出业务异常（各条提示按请求语言拼接）
     *
     * @param groups 校验分组（修改时传 Default 与 Update）
     */
    public static void validate(Validator validator, Object entity, Class<?>... groups) {
        var violations = validator.validate(entity, groups);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            throw new BizException("framework.error.aiCleanseInvalid", msg);
        }
    }

    /** 构造 extra：值为 null 或空白的键不放 */
    public static Map<String, Object> extra(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object v = keyValues[i + 1];
            if (v != null && !(v instanceof String s && s.isBlank())) {
                map.put((String) keyValues[i], v);
            }
        }
        return map;
    }

    /** 去掉首尾空白，空白时为 null */
    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}
