package com.lemon.erp.modules.scm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 主数据被引用检查：返回引用它的单据名称，没有引用返回 null
 */
@Mapper
public interface ScmRefMapper {

    String findMaterial(@Param("id") Long id);

    String findWarehouse(@Param("id") Long id);

    String findUser(@Param("id") Long id);

    String findCustomer(@Param("id") Long id);

    String findSupplier(@Param("id") Long id);

    String findDept(@Param("id") Long id);

    /** 物料分类（分类 ID） */
    String findMaterialCategory(@Param("id") Long id);

    /** 物料改为虚拟商品前：采购类业务（采购订单、请购、询价、供货价格）或发货通知中的引用 */
    String findStockUse(@Param("id") Long id);

    /** 虚拟商品改为实物前：已有的出入库单、出入库草稿 */
    String findVirtualUse(@Param("id") Long id);
}
