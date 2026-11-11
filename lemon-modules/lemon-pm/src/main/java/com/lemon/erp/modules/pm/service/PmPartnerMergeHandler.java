package com.lemon.erp.modules.pm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.mapper.PmProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并：对外项目的客户（本模块不引用供应商）
 */
@Component
@RequiredArgsConstructor
public class PmPartnerMergeHandler implements PartnerMergeHandler {

    private final PmProjectMapper projectMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        if (type != PartnerType.CUSTOMER) {
            return List.of();
        }
        return List.of(new MergeImpact("pm.merge.project",
                projectMapper.selectCount(Wrappers.<PmProject>lambdaQuery().in(PmProject::getCustomerId, sourceIds)), 0));
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type == PartnerType.CUSTOMER) {
            projectMapper.update(Wrappers.<PmProject>lambdaUpdate().set(PmProject::getCustomerId, targetId).in(PmProject::getCustomerId, sourceIds));
        }
    }
}
