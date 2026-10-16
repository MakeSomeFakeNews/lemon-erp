package com.lemon.erp.modules.crm.mapper;

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
public interface CrmJobMapper {

    List<Map<String, Object>> selectFollowDue(@Param("before") LocalDateTime before);

    List<Map<String, Object>> selectIdle(@Param("since") LocalDateTime since);

    int releaseToPool(@Param("ids") List<Long> ids);
}
