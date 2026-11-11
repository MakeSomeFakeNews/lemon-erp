package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import com.lemon.erp.modules.crm.mapper.CrmContactMapper;
import com.lemon.erp.modules.crm.mapper.CrmCustomerMapper;
import com.lemon.erp.modules.crm.mapper.CrmMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 往来单位合并（客户）：联系人、线索、跟进、商机、合同、服务工单改为保留的客户。
 * 经营信息（负责人、阶段、行业等，一个客户一行）：保留的客户已有时保留它的，被合并客户的删除，最近跟进时间取最近的一次；
 * 保留的客户没有时，被合并客户中 ID 最小的一份移过来。保留的客户已有首要联系人时，移过来的联系人不再是首要联系人
 */
@Component
@RequiredArgsConstructor
public class CrmPartnerMergeHandler implements PartnerMergeHandler {

    private final CrmMergeMapper mergeMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        return type == PartnerType.CUSTOMER ? mergeMapper.customerImpacts(sourceIds, targetId) : List.of();
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type != PartnerType.CUSTOMER) {
            return;
        }
        mergeProfile(sourceIds, targetId);
        boolean targetHasPrimary = contactMapper.exists(Wrappers.<CrmContact>lambdaQuery()
                .eq(CrmContact::getCustomerId, targetId).eq(CrmContact::getIsPrimary, 1));
        mergeMapper.mergeContact(sourceIds, targetId, !targetHasPrimary);
        mergeMapper.mergeLead(sourceIds, targetId);
        mergeMapper.mergeFollowup(sourceIds, targetId);
        mergeMapper.mergeOpportunity(sourceIds, targetId);
        mergeMapper.mergeContract(sourceIds, targetId);
        mergeMapper.mergeTicket(sourceIds, targetId);
    }

    private void mergeProfile(List<Long> sourceIds, Long targetId) {
        List<CrmCustomer> sources = customerMapper.selectList(Wrappers.<CrmCustomer>lambdaQuery()
                .in(CrmCustomer::getCustomerId, sourceIds).orderByAsc(CrmCustomer::getId));
        if (sources.isEmpty()) {
            return;
        }
        CrmCustomer target = customerMapper.selectOne(Wrappers.<CrmCustomer>lambdaQuery().eq(CrmCustomer::getCustomerId, targetId));
        List<CrmCustomer> dropped = sources;
        if (target == null) {
            target = sources.get(0);
            dropped = sources.subList(1, sources.size());
            customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate().set(CrmCustomer::getCustomerId, targetId)
                    .eq(CrmCustomer::getId, target.getId()));
        }
        if (dropped.isEmpty()) {
            return;
        }
        LocalDateTime last = dropped.stream().map(CrmCustomer::getLastFollowTime).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        if (last != null && (target.getLastFollowTime() == null || last.isAfter(target.getLastFollowTime()))) {
            customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate().set(CrmCustomer::getLastFollowTime, last)
                    .eq(CrmCustomer::getId, target.getId()));
        }
        customerMapper.deleteByIds(dropped.stream().map(CrmCustomer::getId).toList());
    }
}
