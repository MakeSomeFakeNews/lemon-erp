package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.crm.entity.CrmOpportunityItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CrmOpportunityItemMapper extends BaseMapper<CrmOpportunityItem> {

    List<CrmOpportunityItem> selectByOpportunity(@Param("opportunityId") Long opportunityId);

    void insertBatch(@Param("items") List<CrmOpportunityItem> items);
}
