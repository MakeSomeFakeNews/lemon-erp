package com.lemon.erp.framework.dict;

import com.lemon.erp.common.exception.BizException;

/**
 * 字典查询（system 模块实现）：业务模块校验业务字典的取值。业务字典每个租户各自维护，可以新增取值，
 * 所以保存时按当前租户的字典校验，而不是在代码里写死允许的取值
 */
public interface DictLookup {

    /** 字典中是否有启用的该取值（业务字典按当前租户） */
    boolean contains(String dictType, String value);

    /**
     * 取值必须是字典中启用的值，否则提示「{名称}不正确」；value 为空时不校验（是否必填由调用方决定）
     *
     * @param fieldKey 字段名称的消息键，如 hr.label.empType
     */
    default void check(String dictType, String value, String fieldKey) {
        if (value != null && !value.isBlank() && !contains(dictType, value)) {
            throw new BizException("framework.error.dictValueInvalid", com.lemon.erp.common.i18n.Messages.get(fieldKey), value);
        }
    }
}
