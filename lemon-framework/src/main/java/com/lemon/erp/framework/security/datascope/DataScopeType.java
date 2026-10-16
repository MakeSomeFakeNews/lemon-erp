package com.lemon.erp.framework.security.datascope;

import java.util.Arrays;

/**
 * 角色的数据权限类型，取值与字典 sys_data_scope 一致
 */
public enum DataScopeType {

    ALL(1, "全部数据"),
    CUSTOM(2, "自定义数据"),
    DEPT(3, "本部门数据"),
    DEPT_AND_CHILD(4, "本部门及以下数据"),
    SELF(5, "仅本人数据");

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
