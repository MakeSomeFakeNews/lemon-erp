package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmLead;
import org.apache.ibatis.annotations.Param;

public interface CrmLeadMapper extends BaseMapper<CrmLead> {

    /** @param keyword 编号、公司、联系人或电话 */
    IPage<CrmLead> selectPageList(IPage<CrmLead> page, @Param("keyword") String keyword, @Param("status") String status,
                                  @Param("source") String source, @Param("campaignId") Long campaignId,
                                  @Param("ownerId") Long ownerId);

    CrmLead selectDetail(@Param("id") Long id);
}
