package com.lemon.erp.modules.workflow.model;

/**
 * 条件规则：流程变量 field 与 value 按 operator 比较
 */
public record ConditionRule(String field, ConditionOperator operator, String value) {

    public boolean test(Object actual) {
        return operator.test(actual, value);
    }
}
