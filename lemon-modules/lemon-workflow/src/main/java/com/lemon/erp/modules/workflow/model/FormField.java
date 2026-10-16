package com.lemon.erp.modules.workflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 表单字段，发起流程时按字段类型校验，值保存为同名的流程变量
 *
 * @param field   字段标识，即流程变量名
 * @param options 下拉选项，SELECT 类型使用
 */
public record FormField(String field, String label, FormFieldType type, Boolean required, String placeholder,
                        List<String> options) {

    public FormField {
        options = options == null ? List.of() : options;
    }

    /** 是否必填（未传按非必填处理） */
    @JsonIgnore
    public boolean isMandatory() {
        return Boolean.TRUE.equals(required);
    }
}
