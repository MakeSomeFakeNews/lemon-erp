package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AdjustItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 毛利明细底部的调整行（issue #93，显示方式 {@link com.lemon.erp.modules.fin.model.AdjustMode#BOTTOM}）：金额为负（退货方向相反），
 * 成本为 0，毛利 = 金额；进货报表没有成本和毛利（为空）。taxedAmount 只有整单优惠有（含税的优惠额，取负）
 */
public record AdjustRow(AdjustItem item, BigDecimal amount, BigDecimal taxedAmount, BigDecimal cost, BigDecimal profit) {

    /**
     * 按类别生成调整行，只列出金额不为 0 的项；各项为正数时表示减少销售金额 / 进货金额
     *
     * @param sales 销售（有成本、毛利）还是进货
     */
    public static List<AdjustRow> of(boolean sales, BigDecimal discount, BigDecimal discountTaxed, BigDecimal freight,
                                     BigDecimal settleDiscount, BigDecimal badDebt) {
        List<AdjustRow> rows = new ArrayList<>();
        add(rows, sales, AdjustItem.DISCOUNT, discount, discountTaxed);
        add(rows, sales, AdjustItem.FREIGHT, freight, null);
        add(rows, sales, AdjustItem.SETTLE_DISCOUNT, settleDiscount, null);
        add(rows, sales, AdjustItem.BAD_DEBT, badDebt, null);
        return rows;
    }

    private static void add(List<AdjustRow> rows, boolean sales, AdjustItem item, BigDecimal value, BigDecimal taxed) {
        if (value == null || value.signum() == 0) {
            return;
        }
        BigDecimal amount = value.negate();
        rows.add(new AdjustRow(item, amount, taxed == null ? null : taxed.negate(), sales ? BigDecimal.ZERO.setScale(2) : null,
                sales ? amount : null));
    }
}
