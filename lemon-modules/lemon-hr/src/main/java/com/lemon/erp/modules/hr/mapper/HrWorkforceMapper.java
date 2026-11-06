package com.lemon.erp.modules.hr.mapper;

import com.lemon.erp.modules.hr.service.HrWorkforceSummaryService.Counts;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 人员变动汇总（智能报告）：期初、期末在职人数，期间内入职、离职人数。只统计人数，不涉及薪资等个人信息
 */
public interface HrWorkforceMapper {

    /**
     * @param begin 期间开始（期初人数为 begin 前一天结束时在职的人数）
     * @param end   期间结束（含）
     */
    Counts selectCounts(@Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
