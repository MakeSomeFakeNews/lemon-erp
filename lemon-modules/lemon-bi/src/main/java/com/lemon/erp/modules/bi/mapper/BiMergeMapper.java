package com.lemon.erp.modules.bi.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：数据仓库事实表的客户 / 供应商，下次 ETL 之前的分析也按合并后的单位汇总
 */
@Mapper
public interface BiMergeMapper {

    List<MergeImpact> impacts(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds);

    int mergeSales(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergePurchase(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);
}
