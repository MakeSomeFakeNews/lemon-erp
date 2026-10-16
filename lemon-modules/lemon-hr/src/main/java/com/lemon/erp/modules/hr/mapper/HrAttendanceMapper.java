package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.hr.entity.HrAttendance;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HrAttendanceMapper extends BaseMapper<HrAttendance> {

    /**
     * @param keyword 工号或姓名
     * @param deptIds 部门及下级部门
     */
    IPage<HrAttendance> selectPageList(IPage<HrAttendance> page, @Param("keyword") String keyword,
                                       @Param("deptIds") Collection<Long> deptIds, @Param("status") String status,
                                       @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    List<HrAttendance> selectBetween(@Param("employeeIds") Collection<Long> employeeIds,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);
}
