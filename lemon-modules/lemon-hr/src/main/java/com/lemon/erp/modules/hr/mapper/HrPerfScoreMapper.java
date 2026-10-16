package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrPerfScore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HrPerfScoreMapper extends BaseMapper<HrPerfScore> {

    List<HrPerfScore> selectByAppraisal(@Param("appraisalId") Long appraisalId);

    /** 考核方案的每张考核表、每个指标对应的空白得分（未保存） */
    List<HrPerfScore> selectBlankForPlan(@Param("planId") Long planId);

    /** 批量插入得分，单条 INSERT ... VALUES (...), (...) */
    void insertBatch(@Param("scores") List<HrPerfScore> scores);
}
