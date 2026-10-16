package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrTrainingRecord;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HrTrainingRecordMapper extends BaseMapper<HrTrainingRecord> {

    List<HrTrainingRecord> selectByTraining(@Param("trainingId") Long trainingId);

    /** 员工的参训记录（含培训信息），最近的在前 */
    List<HrTrainingRecord> selectByEmployee(@Param("employeeId") Long employeeId);

    void insertBatch(@Param("trainingId") Long trainingId, @Param("employeeIds") Collection<Long> employeeIds);

    /** 已结束且出勤的培训学时合计 */
    BigDecimal sumHours(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
