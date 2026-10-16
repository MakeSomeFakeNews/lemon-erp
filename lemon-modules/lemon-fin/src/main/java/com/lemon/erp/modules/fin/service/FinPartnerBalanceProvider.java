package com.lemon.erp.modules.fin.service;

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
        BigDecimal v = billMapper.selectUnsettledAmount("AR", customerId);
        BigDecimal advance = advanceMapper.sumUnused("RECEIPT", customerId);
        return (v == null ? BigDecimal.ZERO : v).subtract(advance == null ? BigDecimal.ZERO : advance);
    }
}
