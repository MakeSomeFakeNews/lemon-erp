package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.crm.entity.CrmContract;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CrmContractMapper extends BaseMapper<CrmContract> {

    Page<CrmContract> selectPageList(Page<CrmContract> page, @Param("keyword") String keyword, @Param("status") String status,
                                     @Param("customerId") Long customerId, @Param("opportunityId") Long opportunityId);

    CrmContract selectDetail(@Param("id") Long id);

    /** 执行中的合同（回款提醒） */
    List<CrmContract> selectActive();
}
