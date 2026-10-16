package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pm.entity.PmTask;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PmTaskMapper extends BaseMapper<PmTask> {

    List<PmTask> selectByProject(@Param("projectId") Long projectId, @Param("today") LocalDate today);

    /** 指派给用户的未完成任务（规划中、进行中项目） */
    List<PmTask> selectOpenByAssignee(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** 各用户未完成任务的剩余计划工时 = 计划工时 × (1 - 进度) */
    List<Map<String, Object>> selectOpenHoursByUser();

    /** 任务已审批工时合计 */
    BigDecimal sumApprovedHours(@Param("taskId") Long taskId);
}
