package com.lemon.erp.framework.security.datascope;

import java.util.Arrays;

/**
 * 角色的数据权限类型，取值与字典 sys_data_scope 一致
 */
public enum DataScopeType {

    ALL(1, "framework.enum.DataScopeType.all"),
    CUSTOM(2, "framework.enum.DataScopeType.custom"),
    DEPT(3, "framework.enum.DataScopeType.dept"),
    DEPT_AND_CHILD(4, "framework.enum.DataScopeType.deptAndChild"),
    SELF(5, "framework.enum.DataScopeType.self");

    private final int code;
    private final String label;

    DataScopeType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 未知取值返回 null */
    public static DataScopeType of(Integer code) {
        return code == null ? null : Arrays.stream(values()).filter(t -> t.code == code).findFirst().orElse(null);
    }
}
