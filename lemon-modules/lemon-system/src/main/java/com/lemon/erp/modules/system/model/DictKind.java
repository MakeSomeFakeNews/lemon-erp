package com.lemon.erp.modules.system.model;

/**
 * 字典种类
 */
public enum DictKind {
    /** 系统字典：取值由程序内置，全平台共用，只能改名称、颜色、排序 */
    SYSTEM,
    /** 业务字典：每个租户各自维护，可新增、停用取值（内置取值不能删除） */
    BUSINESS
}
