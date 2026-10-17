package com.lemon.erp.framework.workflow;

import java.util.Arrays;
import java.util.List;

/**
 * 业务单据接入的流程：业务模块把它声明为 Spring Bean，说明发起流程时传入哪些流程变量。
 * 流程设计器据此在条件分支中提供可选的变量；这些流程只能由业务单据发起，「发起申请」中不显示。
 *
 * @param processKey 流程标识，与流程模型的标识一致
 * @param name       业务名称（消息键），如「采购订单审核」；变量的 label 同样是消息键，输出时 {@code Messages.get}
 * @param variables  发起时传入、可用于条件分支的流程变量
 */
public record FlowBusiness(String processKey, String name, List<Variable> variables) {

    public static FlowBusiness of(String processKey, String name, Variable... variables) {
        return new FlowBusiness(processKey, name, List.of(variables));
    }

    /** 流程变量的类型，与流程表单字段类型（FieldType）中的同名取值一致 */
    public enum VariableType {
        /** 数字 */
        NUMBER,
        /** 文本 */
        INPUT,
        /** 下拉选择 */
        SELECT
    }

    /**
     * 流程变量
     *
     * @param type     变量类型，与流程表单字段类型一致
     * @param dictType SELECT 类型的取值来自哪个字典（可选值为字典值）
     * @param options  SELECT 类型不使用字典时的可选值；使用字典时为字典之外额外的可选值（如加班时假期类型为 OVERTIME）
     */
    public record Variable(String field, String label, VariableType type, String dictType, List<String> options) {

        public static Variable number(String field, String label) {
            return new Variable(field, label, VariableType.NUMBER, null, List.of());
        }

        public static Variable text(String field, String label) {
            return new Variable(field, label, VariableType.INPUT, null, List.of());
        }

        /** 取值来自字典，另外还可能取字典之外的几个固定值 */
        public static Variable dict(String field, String label, String dictType, String... extraOptions) {
            return new Variable(field, label, VariableType.SELECT, dictType, List.of(extraOptions));
        }

        public static Variable select(String field, String label, String... options) {
            return new Variable(field, label, VariableType.SELECT, null, List.of(options));
        }

        /** 可选值为枚举的名称 */
        public static Variable select(String field, String label, Enum<?>... options) {
            return new Variable(field, label, VariableType.SELECT, null, Arrays.stream(options).map(Enum::name).toList());
        }
    }
}
