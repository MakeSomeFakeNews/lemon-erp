package com.lemon.erp.modules.bi.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.bi.mapper.BiMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并：销售事实表的客户、采购事实表的供应商（ETL 重新抽取前的分析也按合并后的单位汇总）
 */
@Component
@RequiredArgsConstructor
public class BiPartnerMergeHandler implements PartnerMergeHandler {

    private final BiMergeMapper mergeMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        return mergeMapper.impacts(type == PartnerType.CUSTOMER, sourceIds);
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type == PartnerType.CUSTOMER) {
            mergeMapper.mergeSales(sourceIds, targetId);
        } else {
            mergeMapper.mergePurchase(sourceIds, targetId);
        }
    }
}
