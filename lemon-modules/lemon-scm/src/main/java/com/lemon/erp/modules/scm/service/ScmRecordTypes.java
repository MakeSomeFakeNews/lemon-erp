package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.scm.mapper.ScmTenderMapper;
import com.lemon.erp.modules.scm.model.ScmConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 供应链的业务记录类型（附件、变更历史）：采购订单、销售订单（合同扫描件等），客户、供应商（资质、协议）
 */
@Component
@RequiredArgsConstructor
public class ScmRecordTypes implements RecordTypeProvider {

    private final ScmOrderMapper orderMapper;
    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;
    private final ScmPurchaseContractMapper contractMapper;
    private final ScmTenderMapper tenderMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(
                new RecordType("scm_purchase", "采购订单", "scm:purchase:list", "scm:purchase:edit",
                        ScmConstants.ORDER_RULE, id -> order(id, "PURCHASE")),
                new RecordType("scm_sales", "销售订单", "scm:sales:list", "scm:sales:edit",
                        ScmConstants.ORDER_RULE, id -> order(id, "SALES")),
                new RecordType("scm_customer", "客户", "scm:customer:list", "scm:customer:edit", null, customerMapper::selectById),
                new RecordType("scm_supplier", "供应商", "scm:supplier:list", "scm:supplier:edit", null, supplierMapper::selectById),
                new RecordType("scm_contract", "采购合同", "scm:contract:list", "scm:contract:edit",
                        ScmConstants.CONTRACT_RULE, contractMapper::selectById),
                new RecordType("scm_tender", "招标", "scm:tender:list", "scm:tender:edit", ScmConstants.TENDER_RULE, tenderMapper::selectById));
    }

    private ScmOrder order(long id, String kind) {
        ScmOrder o = orderMapper.selectById(id);
        return o != null && kind.equals(o.getKind()) ? o : null;
    }
}
