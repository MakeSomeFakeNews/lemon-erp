package com.lemon.erp.modules.mes.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 主数据被引用检查：返回引用它的单据名称，没有引用返回 null
 */
@Mapper
public interface MesRefMapper {

    String findMaterial(@Param("id") Long id);

    String findWarehouse(@Param("id") Long id);

    String findSupplier(@Param("id") Long id);

    String findUser(@Param("id") Long id);

    String findDept(@Param("id") Long id);

    /** 物料分类（分类 ID） */
    String findMaterialCategory(@Param("id") Long id);

    /** 物料改为虚拟商品前：BOM、替代料、生产计划、委外、工单中的引用 */
    String findStockUse(@Param("id") Long id);
}
