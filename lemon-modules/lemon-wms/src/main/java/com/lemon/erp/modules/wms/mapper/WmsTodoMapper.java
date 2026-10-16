package com.lemon.erp.modules.wms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

/**
 * 首页待办数量
 */
@Mapper
public interface WmsTodoMapper {

    Map<String, Object> selectCounts(@Param("userId") Long userId, @Param("today") LocalDate today,
                                     @Param("tomorrow") LocalDate tomorrow, @Param("in15") LocalDate in15,
                                     @Param("in30") LocalDate in30);
}
