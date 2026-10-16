package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import org.apache.ibatis.annotations.Param;

public interface CrmOpportunityMapper extends BaseMapper<CrmOpportunity> {

    /**
     * @param keyword 编号、名称或客户名称
     * @param status  OPEN 进行中 / WON / LOST
     */
    IPage<CrmOpportunity> selectPageList(IPage<CrmOpportunity> page, @Param("keyword") String keyword,
                                         @Param("stage") String stage, @Param("status") String status,
                                         @Param("customerId") Long customerId, @Param("ownerId") Long ownerId,
                                         @Param("campaignId") Long campaignId);

    CrmOpportunity selectDetail(@Param("id") Long id);
}
