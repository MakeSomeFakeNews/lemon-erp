package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.mes.mapper.MesMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并：MRP 建议供应商、来料检验、不合格品评审、供应商索赔的供应商（本模块不引用客户）
 */
@Component
@RequiredArgsConstructor
public class MesPartnerMergeHandler implements PartnerMergeHandler {

    private final MesMergeMapper mergeMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        return type == PartnerType.SUPPLIER ? mergeMapper.supplierImpacts(sourceIds) : List.of();
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type != PartnerType.SUPPLIER) {
            return;
        }
        mergeMapper.mergeMrpResult(sourceIds, targetId);
        mergeMapper.mergeInspection(sourceIds, targetId);
        mergeMapper.mergeNcr(sourceIds, targetId);
        mergeMapper.mergeSupplierClaim(sourceIds, targetId);
    }
}
