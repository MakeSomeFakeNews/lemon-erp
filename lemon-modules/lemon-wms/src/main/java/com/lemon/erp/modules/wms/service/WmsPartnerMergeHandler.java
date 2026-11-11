package com.lemon.erp.modules.wms.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.wms.mapper.WmsMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并：寄售仓的客户 / 供应商、出入库单和库存流水的往来单位（含名称快照）、来料检验暂存的供应商
 */
@Component
@RequiredArgsConstructor
public class WmsPartnerMergeHandler implements PartnerMergeHandler {

    private final WmsMergeMapper mergeMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        return mergeMapper.impacts(type == PartnerType.CUSTOMER, sourceIds);
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        boolean customer = type == PartnerType.CUSTOMER;
        mergeMapper.mergeWarehouse(customer, sourceIds, targetId);
        mergeMapper.mergeStockFlow(customer, sourceIds, targetId, targetName);
        mergeMapper.mergeStockDoc(customer, sourceIds, targetId, targetName);
        if (!customer) {
            mergeMapper.mergeStockHold(sourceIds, targetId);
        }
    }
}
