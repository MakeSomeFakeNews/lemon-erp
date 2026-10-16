package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrEmployeeChange;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HrEmployeeChangeMapper extends BaseMapper<HrEmployeeChange> {

    List<HrEmployeeChange> selectByEmployee(@Param("employeeId") Long employeeId);
}
