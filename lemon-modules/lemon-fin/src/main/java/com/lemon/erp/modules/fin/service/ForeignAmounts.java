package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.FinConstants;

import java.math.BigDecimal;

/**
 * 外币核销的本位币折算：收付款核销、折让、预收预付冲抵都按单据的记账汇率折算，
 * 原币核销完时取剩余本位币（避免尾差）；与收付汇率的差额由调用方计入汇兑损益
 */
final class ForeignAmounts {

    private ForeignAmounts() {
    }

    /** 是否外币（空视为本位币） */
    static boolean isForeign(String currency) {
        return currency != null && !FinConstants.BASE_CURRENCY.equals(currency);
    }

    /**
     * 原币核销金额、原币折让对应的记账本位币
     *
     * @param fc         核销原币金额（红字单据为负数）
     * @param discountFc 折让原币金额，没有为 0
     * @return [核销本位币, 折让本位币]
     */
    static BigDecimal[] toBase(FinBill bill, BigDecimal fc, BigDecimal discountFc) {
        if (!isForeign(bill.getCurrency())) {
            return new BigDecimal[]{fc, discountFc};
        }
        BigDecimal rate = bill.getExchangeRate();
        BigDecimal discountBase = discountFc.signum() == 0 ? BigDecimal.ZERO : Decimals.money(discountFc.multiply(rate));
        BigDecimal remainingFc = Decimals.nz(bill.getAmountFc()).subtract(Decimals.nz(bill.getSettledFc()));
        if (fc.add(discountFc).compareTo(remainingFc) == 0) {
            // 核销完：本位币取剩余，尾差计入核销金额（只有折让时计入折让）
            BigDecimal remainingBase = bill.getAmount().subtract(Decimals.nz(bill.getSettledAmount()));
            if (fc.signum() == 0) {
                return new BigDecimal[]{BigDecimal.ZERO, remainingBase};
            }
            return new BigDecimal[]{remainingBase.subtract(discountBase), discountBase};
        }
        return new BigDecimal[]{fc.signum() == 0 ? BigDecimal.ZERO : Decimals.money(fc.multiply(rate)), discountBase};
    }
}
