package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.framework.finance.PartnerBalanceProvider;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class FinPartnerBalanceProvider implements PartnerBalanceProvider {

    private final FinBillMapper billMapper;
    private final com.lemon.erp.modules.fin.mapper.FinAdvanceMapper advanceMapper;

    /** 应收余额 = 未核销的应收单 - 未冲抵的预收款 */
    @Override
    public BigDecimal receivableBalance(Long customerId) {
        BigDecimal v = billMapper.selectUnsettledAmount(BillKind.AR, customerId);
        BigDecimal advance = advanceMapper.sumUnused(SettleKind.RECEIPT, customerId);
        return Decimals.nz(v).subtract(Decimals.nz(advance));
    }
}
