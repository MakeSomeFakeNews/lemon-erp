package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.fin.mapper.FinMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并：应收应付单、收付款、预收预付、冲账单、发票、其他收支、票据及其操作记录、应收应付对冲、供应商对账单的往来单位，
 * 以及凭证分录的客户 / 供应商辅助核算（含已记账的凭证，辅助余额表、往来汇总、对账单、账龄随之按合并后的单位汇总）。
 * 单据金额、核销关系、币种不变（币种记在每张单据上，往来单位没有币种，不同币种的单据合并后仍按各自币种核销）
 */
@Component
@RequiredArgsConstructor
public class FinPartnerMergeHandler implements PartnerMergeHandler {

    private final FinMergeMapper mergeMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        return mergeMapper.impacts(type == PartnerType.CUSTOMER, sourceIds);
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        boolean customer = type == PartnerType.CUSTOMER;
        mergeMapper.mergeBill(customer, sourceIds, targetId);
        mergeMapper.mergeSettlement(customer, sourceIds, targetId);
        mergeMapper.mergeAdvance(customer, sourceIds, targetId);
        mergeMapper.mergeOffset(customer, sourceIds, targetId);
        mergeMapper.mergeInvoice(customer, sourceIds, targetId);
        mergeMapper.mergeIoDoc(customer, sourceIds, targetId);
        mergeMapper.mergeNote(customer, sourceIds, targetId);
        mergeMapper.mergeNoteLog(customer, sourceIds, targetId);
        mergeMapper.mergeVoucherEntry(customer, sourceIds, targetId);
        mergeMapper.mergeNetting(customer, sourceIds, targetId);
        if (!customer) {
            mergeMapper.mergeSupplierStatement(sourceIds, targetId);
        }
    }
}
