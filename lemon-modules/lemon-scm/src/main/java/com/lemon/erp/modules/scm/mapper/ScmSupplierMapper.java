package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.SupplierKpi;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface ScmSupplierMapper extends BaseMapper<ScmSupplier> {

    /** 供应商绩效：订单数、入库金额、准时交货率、退货率、最近一次评估，按业务日期范围统计 */
    IPage<SupplierKpi> selectKpi(IPage<SupplierKpi> page, @Param("keyword") String keyword,
                                 @Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
