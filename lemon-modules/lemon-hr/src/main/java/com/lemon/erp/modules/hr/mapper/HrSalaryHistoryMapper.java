package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrSalary;
import com.lemon.erp.modules.hr.entity.HrSalaryHistory;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HrSalaryHistoryMapper extends BaseMapper<HrSalaryHistory> {

    /** 各员工在 date 当天生效的薪资（生效日期不晚于 date 的最近一条），没有历史的员工不在结果中 */
    List<HrSalary> selectEffective(@Param("employeeIds") Collection<Long> employeeIds, @Param("date") LocalDate date);
}
