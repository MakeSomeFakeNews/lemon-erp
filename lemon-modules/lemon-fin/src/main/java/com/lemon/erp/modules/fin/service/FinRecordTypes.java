package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.mapper.FinInvoiceMapper;
import com.lemon.erp.modules.fin.mapper.FinSupplierStatementMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.StatementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 财务业务记录类型（附件、变更历史）：凭证（原始凭证、发票扫描件）、供应商对账单（发票，财务与供应商门户共用）、发票（版式文件）
 */
@Component
@RequiredArgsConstructor
public class FinRecordTypes implements RecordTypeProvider {

    private final FinVoucherMapper voucherMapper;
    private final FinSupplierStatementMapper statementMapper;
    private final FinInvoiceMapper invoiceMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("fin_voucher", "凭证", "fin:voucher:list", "fin:voucher:edit",
                        null, voucherMapper::selectById),
                // 对账单的发票等附件：财务与供应商门户共用
                new RecordType("fin_statement", "供应商对账单", "fin:statement:list,portal:statement:list",
                        "fin:statement:edit,portal:statement:confirm", null, this::statement),
                // 发票的版式文件（PDF / OFD）、XML
                new RecordType("fin_invoice", "发票", "fin:invoice:list", "fin:invoice:edit", null, invoiceMapper::selectById));
    }

    /** 供应商账号只能访问本供应商已发送的对账单 */
    private FinSupplierStatement statement(long id) {
        FinSupplierStatement st = statementMapper.selectById(id);
        if (st == null) {
            return null;
        }
        Long supplierId = SecurityUtils.getLoginUser().supplierId();
        return supplierId == null || (supplierId.equals(st.getSupplierId()) && st.getStatus() != StatementStatus.DRAFT) ? st : null;
    }
}
