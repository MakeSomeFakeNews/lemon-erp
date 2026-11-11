package com.lemon.erp.modules.crm.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：本模块引用客户的数据（经营信息、联系人、线索、跟进、商机、合同、服务工单）
 */
@Mapper
public interface CrmMergeMapper {

    List<MergeImpact> customerImpacts(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    /** 保留的客户已有首要联系人时，被合并客户的联系人不再是首要联系人 */
    int mergeContact(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId, @Param("keepPrimary") boolean keepPrimary);

    int mergeLead(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    /** 跟进记录的所属客户，以及跟进对象为客户的记录 */
    int mergeFollowup(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeOpportunity(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeContract(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeTicket(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);
}
