package com.lemon.erp.modules.hr.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时任务使用的查询
 */
@Mapper
public interface HrJobMapper {

    List<Map<String, Object>> selectProbationDue(@Param("before") LocalDate before);

    int applyEffectiveSalary(@Param("today") LocalDate today);
}
