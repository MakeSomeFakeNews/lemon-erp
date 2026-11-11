package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import com.lemon.erp.modules.hr.model.AttendanceStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HrAttendanceMapper extends BaseMapper<HrAttendance> {

    /**
     * @param keyword 工号或姓名
     * @param deptIds 部门及下级部门
     * @param awaitingDay 不为空时排除这一天已打上班卡、还没打下班卡的记录（按缺卡筛选时传今天）
     */
    IPage<HrAttendance> selectPageList(IPage<HrAttendance> page, @Param("keyword") String keyword,
                                       @Param("deptIds") Collection<Long> deptIds, @Param("status") AttendanceStatus status,
                                       @Param("awaitingDay") LocalDate awaitingDay,
                                       @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    List<HrAttendance> selectBetween(@Param("employeeIds") Collection<Long> employeeIds,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);
}
