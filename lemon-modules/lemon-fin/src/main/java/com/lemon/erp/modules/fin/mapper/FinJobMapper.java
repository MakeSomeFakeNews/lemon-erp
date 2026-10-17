package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.model.BillKind;
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
public interface FinJobMapper {

    Map<String, Object> selectOverdueSummary(@Param("kind") BillKind kind, @Param("today") LocalDate today);
}
