package com.lemon.erp.framework.ref;

/**
 * 被引用的主数据类型
 */
public enum RefType {
    MATERIAL,
    WAREHOUSE,
    CUSTOMER,
    SUPPLIER,
    USER,
    DEPT,
    /** 物料分类（ID 为 wms_material_category.id，引用处按分类编码关联） */
    MATERIAL_CATEGORY
}
