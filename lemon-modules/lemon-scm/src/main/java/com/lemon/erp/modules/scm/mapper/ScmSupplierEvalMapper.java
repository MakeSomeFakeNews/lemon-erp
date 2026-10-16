package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmSupplierEval;
import org.apache.ibatis.annotations.Param;

public interface ScmSupplierEvalMapper extends BaseMapper<ScmSupplierEval> {

    IPage<ScmSupplierEval> selectPageList(IPage<ScmSupplierEval> page, @Param("supplierId") Long supplierId,
                                          @Param("period") String period);
}
