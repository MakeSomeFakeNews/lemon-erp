package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmCustomerPriceMapper extends BaseMapper<ScmCustomerPrice> {

    /** 客户的执行价（含物料编码名称），按物料编码排序 */
    List<ScmCustomerPrice> selectByCustomer(@Param("customerId") Long customerId);

    /**
     * 导出：执行价及客户、物料的编码名称，按客户编码、物料编码排序
     *
     * @param customerId 为空时全部客户
     * @param keyword    客户编码 / 名称，模糊匹配，可为空
     */
    List<ScmCustomerPrice> selectExport(@Param("customerId") Long customerId, @Param("keyword") String keyword, @Param("limit") int limit);

    /** 多值插入 */
    int insertBatch(@Param("list") List<ScmCustomerPrice> list);
}
