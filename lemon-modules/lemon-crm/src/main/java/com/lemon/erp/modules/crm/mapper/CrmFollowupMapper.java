package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmFollowup;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface CrmFollowupMapper extends BaseMapper<CrmFollowup> {

    /**
     * @param customerId 客户（含该客户商机的跟进）
     * @param userId     跟进人
     */
    IPage<CrmFollowup> selectPageList(IPage<CrmFollowup> page, @Param("targetType") String targetType,
                                      @Param("targetId") Long targetId, @Param("customerId") Long customerId,
                                      @Param("userId") Long userId, @Param("method") String method,
                                      @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
