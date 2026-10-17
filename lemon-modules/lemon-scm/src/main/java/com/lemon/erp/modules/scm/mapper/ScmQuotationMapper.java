package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.scm.entity.ScmQuotation;
import com.lemon.erp.modules.scm.entity.ScmQuotationItem;
import com.lemon.erp.modules.scm.model.QuotationStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmQuotationMapper extends BaseMapper<ScmQuotation> {

    Page<ScmQuotation> selectPageList(Page<ScmQuotation> page, @Param("keyword") String keyword, @Param("status") QuotationStatus status,
                                      @Param("customerId") Long customerId, @Param("opportunityId") Long opportunityId);

    ScmQuotation selectDetail(@Param("id") Long id);

    List<ScmQuotationItem> selectItems(@Param("quotationId") Long quotationId);
}
