package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmCampaign;
import org.apache.ibatis.annotations.Param;

public interface CrmCampaignMapper extends BaseMapper<CrmCampaign> {

    /** 活动及效果：线索数、已转化、商机数、赢单金额 */
    IPage<CrmCampaign> selectPageList(IPage<CrmCampaign> page, @Param("keyword") String keyword,
                                      @Param("type") String type, @Param("status") String status);

    CrmCampaign selectDetail(@Param("id") Long id);
}
