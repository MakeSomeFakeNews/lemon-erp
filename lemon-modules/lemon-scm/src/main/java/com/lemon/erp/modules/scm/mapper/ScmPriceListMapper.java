package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ScmPriceListMapper extends BaseMapper<ScmPriceList> {

    IPage<ScmPriceList> selectPageList(IPage<ScmPriceList> page, @Param("keyword") String keyword, @Param("level") String level);

    /** 取价：客户专属价优先，其次客户等级精确匹配，数量达到的最高一档，在有效期内 */
    ScmPriceList selectQuote(@Param("materialId") Long materialId, @Param("customerId") Long customerId, @Param("level") String level,
                             @Param("qty") BigDecimal qty, @Param("date") LocalDate date);
}
