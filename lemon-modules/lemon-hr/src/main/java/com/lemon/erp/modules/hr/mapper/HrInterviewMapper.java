package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrInterview;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HrInterviewMapper extends BaseMapper<HrInterview> {

    List<HrInterview> selectByCandidate(@Param("candidateId") Long candidateId);
}
