package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：本模块引用供应商的数据（MRP 建议供应商、来料检验、不合格品、供应商索赔）
 */
@Mapper
public interface MesMergeMapper {

    List<MergeImpact> supplierImpacts(@Param("ids") List<Long> sourceIds);

    int mergeMrpResult(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeInspection(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeNcr(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeSupplierClaim(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);
}
