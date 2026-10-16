package com.lemon.erp.framework.log;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型，code 与字典 sys_oper_type 对应
 */
@Getter
@AllArgsConstructor
public enum OperType {
    OTHER(0),
    INSERT(1),
    UPDATE(2),
    DELETE(3),
    GRANT(4),
    EXPORT(5),
    FORCE(6),
    CLEAN(7),
    IMPORT(8);

    private final int code;
}
