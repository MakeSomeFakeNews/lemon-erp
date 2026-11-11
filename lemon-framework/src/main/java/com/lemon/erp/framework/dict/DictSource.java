package com.lemon.erp.framework.dict;

import java.util.Map;

/**
 * 由业务模块维护数据的「字典」：取值不在字典表中，而在业务模块自己的表里（如物料分类由仓库模块的分类表维护，支持多级）。
 * 按类型读取字典的地方（前端下拉框和标签 {@code /system/dict/data/type/{type}}、{@link DictLookup}、按字典显示名称的报表）
 * 遇到这里登记的类型时改为从实现取值，因此原来按字典使用的代码不需要修改；数据的维护在业务模块自己的页面中进行，
 * 不出现在「字典管理」「业务字典」中。由业务模块实现并注册为 Bean
 */
public interface DictSource {

    /** 字典类型，如 wms_material_category */
    String dictType();

    /** 当前租户启用的取值 → 显示名称（按显示顺序） */
    Map<String, String> options();
}
