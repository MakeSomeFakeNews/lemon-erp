package com.lemon.erp.framework.setting;

/** 参数的值类型，决定参数设置页面的输入控件和保存时的校验（DECIMAL、TABLE 只用于业务参数） */
public enum SettingType {
    TEXT,
    NUMBER,
    BOOL,
    SELECT,
    /** 小数 */
    DECIMAL,
    /** 表格（多行多列的数字，如税率表） */
    TABLE
}
