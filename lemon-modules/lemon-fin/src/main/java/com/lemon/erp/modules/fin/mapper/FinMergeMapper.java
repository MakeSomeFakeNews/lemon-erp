package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：本模块引用客户 / 供应商的数据的影响范围与迁移。
 * customer 为 true 时处理客户，否则处理供应商；partner_id 同时存放客户和供应商的表按种类区分
 * （应收应付单 AR / AP、收付款 RECEIPT / PAYMENT、发票 OUTPUT / INPUT、票据 RECEIVABLE / PAYABLE、其他收支的关联单位类型等）
 */
@Mapper
public interface FinMergeMapper {

    List<MergeImpact> impacts(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds);

    int mergeBill(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeSettlement(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeAdvance(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeOffset(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeInvoice(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeIoDoc(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeNote(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeNoteLog(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    /** 凭证分录的辅助核算（客户 / 供应商），含已记账、已结账期间的凭证：辅助余额按合并后的单位汇总 */
    int mergeVoucherEntry(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeNetting(@Param("customer") boolean customer, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    int mergeSupplierStatement(@Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);
}
