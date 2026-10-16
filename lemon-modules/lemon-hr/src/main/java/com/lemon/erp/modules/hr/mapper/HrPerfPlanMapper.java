package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrPerfPlan;
import org.apache.ibatis.annotations.Param;

public interface HrPerfPlanMapper extends BaseMapper<HrPerfPlan> {

    IPage<HrPerfPlan> selectPageList(IPage<HrPerfPlan> page, @Param("keyword") String keyword, @Param("status") String status);

    HrPerfPlan selectDetail(@Param("id") Long id);
}
