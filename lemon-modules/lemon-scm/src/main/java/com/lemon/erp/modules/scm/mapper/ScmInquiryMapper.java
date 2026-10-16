package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.scm.entity.ScmInquiry;
import com.lemon.erp.modules.scm.entity.ScmInquiryItem;
import com.lemon.erp.modules.scm.entity.ScmInquiryQuote;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmInquiryMapper extends BaseMapper<ScmInquiry> {

    Page<ScmInquiry> selectPageList(Page<ScmInquiry> page, @Param("keyword") String keyword, @Param("status") String status);

    ScmInquiry selectDetail(@Param("id") Long id);

    List<ScmInquiryItem> selectItems(@Param("inquiryId") Long inquiryId);

    List<ScmInquiryQuote> selectQuotes(@Param("inquiryId") Long inquiryId);
}
