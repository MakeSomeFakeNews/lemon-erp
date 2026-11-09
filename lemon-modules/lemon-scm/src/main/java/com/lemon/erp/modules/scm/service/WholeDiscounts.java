package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 整单优惠（含抹零，含税金额）按行金额比例分摊到各行（纯计算）。
 * <p>
 * 各行减少 round2(优惠 × 行金额 / 合计)，尾差放在金额最大的一行，分摊后的行金额合计精确等于 原合计 − 优惠；
 * 行金额直接保存，单价 = 行金额 / 数量（4 位小数，仅供展示和部分出入库时按单价计算），原价记入 origPrice。
 * 在促销、返利抵扣之后执行（先促销、再返利、最后整单优惠）
 */
public final class WholeDiscounts {

    private WholeDiscounts() {
    }

    /**
     * 按金额比例分摊
     *
     * @param amounts  各行金额（不小于 0）
     * @param discount 优惠金额，为空或 0 时原样返回
     * @return 分摊后的各行金额，合计 = 原合计 − 优惠
     */
    public static List<BigDecimal> allocate(List<BigDecimal> amounts, BigDecimal discount) {
        BigDecimal off = Decimals.money(discount);
        if (off.signum() == 0) {
            return new ArrayList<>(amounts);
        }
        if (off.signum() < 0) {
            throw new BizException("scm.error.wholeDiscountNegative");
        }
        BigDecimal total = Decimals.sum(amounts, a -> a);
        if (off.compareTo(total) > 0) {
            throw new BizException("scm.error.wholeDiscountExceedsTotal", total.stripTrailingZeros().toPlainString());
        }
        int largest = 0;
        for (int i = 1; i < amounts.size(); i++) {
            if (amounts.get(i).compareTo(amounts.get(largest)) > 0) {
                largest = i;
            }
        }
        List<BigDecimal> result = new ArrayList<>(amounts.size());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal lineOff = i == largest ? BigDecimal.ZERO
                    : off.multiply(amounts.get(i)).divide(total, 2, RoundingMode.HALF_UP).min(amounts.get(i));
            allocated = allocated.add(lineOff);
            result.add(amounts.get(i).subtract(lineOff));
        }
        BigDecimal rest = result.get(largest).subtract(off.subtract(allocated));
        if (rest.signum() < 0) {
            // 尾差超过最大行金额（只在优惠接近合计且各行很小时出现）：依次从其他行补足
            BigDecimal lack = rest.negate();
            rest = BigDecimal.ZERO;
            for (int i = 0; i < result.size() && lack.signum() > 0; i++) {
                BigDecimal take = result.get(i).min(lack);
                result.set(i, result.get(i).subtract(take));
                lack = lack.subtract(take);
            }
        }
        result.set(largest, rest);
        return result;
    }

    /**
     * 订单明细应用整单优惠：改写行金额、税额、单价，原价为空时记入优惠前的单价，分摊额记入 discountShare
     *
     * @param items    明细（金额已按促销、返利后的单价计算），赠品金额为 0 不参与
     * @param discount 整单优惠，为空或 0 不处理
     */
    public static void apply(List<ScmOrderItem> items, BigDecimal discount) {
        if (discount == null || discount.signum() == 0) {
            return;
        }
        List<BigDecimal> amounts = allocate(items.stream().map(ScmOrderItem::getAmount).toList(), discount);
        for (int i = 0; i < items.size(); i++) {
            ScmOrderItem item = items.get(i);
            BigDecimal amount = amounts.get(i);
            if (amount.compareTo(item.getAmount()) == 0) {
                continue;
            }
            if (item.getOrigPrice() == null) {
                item.setOrigPrice(item.getPrice());
            }
            item.setDiscountShare(Decimals.nz(item.getDiscountShare()).add(item.getAmount().subtract(amount)));
            item.setAmount(amount);
            item.setTaxAmount(tax(amount, item.getTaxRate()));
            item.setPrice(item.getQty().signum() == 0 ? item.getPrice() : amount.divide(item.getQty(), 4, RoundingMode.HALF_UP));
        }
    }
}
