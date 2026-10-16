package com.lemon.erp.modules.mes.mapper;

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
public interface MesJobMapper {

    List<Map<String, Object>> selectMaintainDue(@Param("before") LocalDate before);

    List<Map<String, Object>> selectOverdueWorkOrders(@Param("today") LocalDate today);
}
