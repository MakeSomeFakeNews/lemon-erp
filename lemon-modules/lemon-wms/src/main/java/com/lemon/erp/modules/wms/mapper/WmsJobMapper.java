package com.lemon.erp.modules.wms.mapper;

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
public interface WmsJobMapper {

    List<Map<String, Object>> selectExpiringBatches(@Param("before") LocalDate before);
}
