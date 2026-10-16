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
}
