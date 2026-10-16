package com.lemon.erp.modules.bi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.bi.entity.BiReport;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BiReportMapper extends BaseMapper<BiReport> {

    /** 本人创建的和共享的报表 */
    List<BiReport> selectVisible(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("cube") String cube);
}
