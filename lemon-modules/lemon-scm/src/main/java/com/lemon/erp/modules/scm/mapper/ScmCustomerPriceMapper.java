package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmCustomerPriceMapper extends BaseMapper<ScmCustomerPrice> {

    /** 客户的执行价（含物料编码名称），按物料编码排序 */
    List<ScmCustomerPrice> selectByCustomer(@Param("customerId") Long customerId);

    /** 多值插入 */
    int insertBatch(@Param("list") List<ScmCustomerPrice> list);
}
