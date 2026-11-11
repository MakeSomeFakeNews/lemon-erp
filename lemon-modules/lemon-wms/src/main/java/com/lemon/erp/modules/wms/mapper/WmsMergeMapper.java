package com.lemon.erp.modules.wms.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：本模块引用客户 / 供应商的数据的影响范围与迁移。
 * customer 为 true 时处理客户，否则处理供应商：寄售仓按仓库类型、出入库单按业务类型区分，流水按所属出入库单区分
 */
@Mapper
public interface WmsMergeMapper {

    List<MergeImpact> impacts(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds);

    int mergeWarehouse(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    /** 流水在出入库单之前迁移（按出入库单的业务类型区分） */
    int mergeStockFlow(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId,
                       @Param("targetName") String targetName);

    int mergeStockDoc(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId,
                      @Param("targetName") String targetName);

    /** 来料检验暂存（只有供应商） */
    int mergeStockHold(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);
}
