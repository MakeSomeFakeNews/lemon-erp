package com.lemon.erp.modules.scm.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 往来单位合并（{@code PartnerMergeHandler}）：本模块引用客户 / 供应商的单据的影响范围、冲突检查与迁移。
 * 价格（客户执行价、供货价格、价格记忆）有唯一约束，按规则在 Service 中合并，不在这里
 */
@Mapper
public interface ScmMergeMapper {

    /** 同一类数据可能分几行返回（如承运商分布在出入库单、草稿、发运单），调用方按消息键汇总 */
    List<MergeImpact> customerImpacts(@Param("ids") List<Long> sourceIds);

    List<MergeImpact> supplierImpacts(@Param("ids") List<Long> sourceIds);

    /**
     * 迁移一类引用
     *
     * @param ref 客户：salesOrder、salesTradeDoc、salesDraft、quotation、delivery、shipment、salesRebate、channelReport、priceList；
     *            供应商：purchaseOrder、purchaseTradeDoc、purchaseDraft、carrierTradeDoc、carrierDraft、carrierShipment、requisition、
     *            inquiryQuote、inquirySupplier、supplierEval、purchaseRebate、purchaseContract、tenderAward、tenderSupplier、bid、asn
     */
    int mergeRef(@Param("ref") String ref, @Param("ids") List<Long> sourceIds, @Param("targetId") Long targetId);

    /** 两份以上资料（ids 含保留的资料）参与了同一张询价单：返回询价单号 */
    List<String> sharedInquiries(@Param("ids") List<Long> ids);

    /** 两份以上资料被邀请或投标了同一次招标：返回招标编号 */
    List<String> sharedTenders(@Param("ids") List<Long> ids);

    /** 两份以上资料（经销商）上报了同一期间的渠道库存：返回期间 yyyyMM */
    List<Integer> sharedChannelPeriods(@Param("ids") List<Long> ids);
}
