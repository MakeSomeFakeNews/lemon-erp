package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.QmsCapaStep;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QmsCapaStepMapper extends BaseMapper<QmsCapaStep> {

    List<QmsCapaStep> selectByCapa(@Param("capaId") Long capaId);

    /** 批量插入，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("steps") List<QmsCapaStep> steps);
}
