package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrTraining;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface HrTrainingMapper extends BaseMapper<HrTraining> {

    IPage<HrTraining> selectPageList(IPage<HrTraining> page, @Param("keyword") String keyword,
                                     @Param("category") String category, @Param("status") String status,
                                     @Param("yearStart") LocalDate yearStart, @Param("yearEnd") LocalDate yearEnd);
}
