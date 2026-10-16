package com.lemon.erp.modules.workflow.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * 条件运算符。两边都是数字时按数值比较，否则按字符串比较（yyyy-MM-dd 格式的日期按字符串比较即可）。
 * 新增运算符只需在这里加一个枚举值，前端同步增加选项
 */
public enum ConditionOperator {

    EQ((a, e) -> compare(a, e) == 0),
    NE((a, e) -> compare(a, e) != 0),
    GT((a, e) -> compare(a, e) > 0),
    GE((a, e) -> compare(a, e) >= 0),
    LT((a, e) -> compare(a, e) < 0),
    LE((a, e) -> compare(a, e) <= 0),
    /** 属于其中之一，多个值用逗号分隔 */
    IN((a, e) -> Arrays.stream(e.split("[,，]")).map(String::trim).anyMatch(v -> compare(a, v) == 0)),
    /** 文本包含 */
    CONTAINS((a, e) -> a.toString().contains(e));

    private final BiPredicate<Object, String> predicate;

    ConditionOperator(BiPredicate<Object, String> predicate) {
        this.predicate = predicate;
    }

    /**
     * @param actual   流程变量的值；为 null（未填写）时只有 NE 成立
     * @param expected 设计时填写的比较值
     */
    public boolean test(Object actual, String expected) {
        if (actual == null || expected == null) {
            return this == NE && !Objects.equals(actual, expected);
        }
        return predicate.test(actual, expected);
    }

    private static int compare(Object actual, String expected) {
        BigDecimal left = toNumber(actual);
        BigDecimal right = toNumber(expected);
        if (left != null && right != null) {
            return left.compareTo(right);
        }
        return actual.toString().compareTo(expected);
    }

    private static BigDecimal toNumber(Object value) {
        if (value instanceof Boolean) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
