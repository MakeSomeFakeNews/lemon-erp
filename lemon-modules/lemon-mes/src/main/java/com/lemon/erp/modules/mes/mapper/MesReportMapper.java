package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesReport;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface MesReportMapper extends BaseMapper<MesReport> {

    IPage<MesReport> selectPageList(IPage<MesReport> page, @Param("woId") Long woId,
                                    @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);
}
