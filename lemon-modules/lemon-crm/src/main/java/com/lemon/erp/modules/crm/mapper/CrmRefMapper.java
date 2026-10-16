package com.lemon.erp.modules.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 主数据被引用检查：返回引用它的单据名称，没有引用返回 null
 */
@Mapper
public interface CrmRefMapper {

    String findMaterial(@Param("id") Long id);

    String findCustomer(@Param("id") Long id);

    String findUser(@Param("id") Long id);

    String findDept(@Param("id") Long id);
}
