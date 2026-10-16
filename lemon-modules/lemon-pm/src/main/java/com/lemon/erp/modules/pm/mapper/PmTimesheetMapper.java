package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pm.entity.PmTimesheet;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PmTimesheetMapper extends BaseMapper<PmTimesheet> {

    IPage<PmTimesheet> selectPageList(IPage<PmTimesheet> page, @Param("projectId") Long projectId, @Param("userId") Long userId,
                                      @Param("status") String status, @Param("beginDate") LocalDate beginDate,
                                      @Param("endDate") LocalDate endDate);

    /** 本人某段时间的工时 */
    List<PmTimesheet> selectMine(@Param("userId") Long userId, @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 本人某天已填报的工时合计（不含驳回的、不含 excludeId） */
    BigDecimal sumDay(@Param("userId") Long userId, @Param("workDate") LocalDate workDate, @Param("excludeId") Long excludeId);
}
