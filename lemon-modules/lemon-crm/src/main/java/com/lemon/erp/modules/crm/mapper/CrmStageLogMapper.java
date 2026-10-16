package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.crm.entity.CrmStageLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CrmStageLogMapper extends BaseMapper<CrmStageLog> {

    List<CrmStageLog> selectByOpportunity(@Param("opportunityId") Long opportunityId);
}
