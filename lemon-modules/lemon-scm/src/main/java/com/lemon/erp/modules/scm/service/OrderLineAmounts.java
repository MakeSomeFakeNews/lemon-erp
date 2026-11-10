package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;

/**
 * 订单行金额的分批计算与变更重新分摊（纯计算，金额均为订单币种的含税金额）。
 * <ul>
 *     <li>出入库：完成订单行的那一批（本次数量不少于未完成数量）取 订单行金额 − 已出入库金额（超出部分按单价加上），
 *     其余按 数量 × 单价，保证累计金额精确等于订单行金额（整单优惠分摊后行金额不一定等于 数量 × 单价）；</li>
 *     <li>退货：退回全部实际完成数量时取剩余的已出入库金额，其余按 数量 × 单价（不超过剩余金额）；补发的退货扣减已出入库金额，
 *     补发时最后一批取回差额，退款的退货另计已退款金额；</li>
 *     <li>订单变更：已出入库的部分（数量、金额、已分摊的优惠）不变，未出入库的部分按变更后的单价计算，
 *     有整单优惠时把剩余的优惠按未出入库部分的金额比例重新分摊。</li>
 * </ul>
 */
public final class OrderLineAmounts {

    private OrderLineAmounts() {
    }

    /**
     * 出入库（非退货）本行金额
     *
     * @param item 订单行，回写数量之后重新读取的值：doneQty 已含本次数量，doneAmount 不含本次金额
     * @param qty  本次数量
     */
    public static BigDecimal outbound(ScmOrderItem item, BigDecimal qty) {
        BigDecimal byPrice = amount(qty, item.getPrice());
        if (item.getAmount() == null) {
            return byPrice;
        }
        BigDecimal remaining = item.getQty().subtract(item.getDoneQty().subtract(qty)).add(Decimals.nz(item.getReplenishQty()));
        if (remaining.signum() <= 0 || qty.compareTo(remaining) < 0) {
            return byPrice;
        }
        BigDecimal rest = item.getAmount().subtract(Decimals.nz(item.getDoneAmount()));
        if (rest.signum() < 0) {
            return byPrice;
        }
        return rest.add(amount(qty.subtract(remaining), item.getPrice()));
    }

    /**
     * 退货本行金额
     *
     * @param item 订单行，回写退货数量之后重新读取的值：returnQty 已含本次数量，金额不含本次
     * @param qty  本次退货数量
     */
    public static BigDecimal returned(ScmOrderItem item, BigDecimal qty) {
        BigDecimal netBefore = item.getDoneQty().subtract(item.getReturnQty()).add(qty);
        BigDecimal net = Decimals.nz(item.getDoneAmount()).subtract(Decimals.nz(item.getReturnAmount()));
        BigDecimal byPrice = amount(qty, item.getPrice());
        if (net.signum() <= 0) {
            return byPrice;
        }
        return qty.compareTo(netBefore) >= 0 ? net : byPrice.min(net);
    }

    /**
     * 订单关闭时本行的尾差调整：部分出入库的行（0 &lt; 已出入库数量 &lt; 订单数量）应得的金额减去已出入库金额；
     * 未出入库、已出完（含超发）的行没有尾差，返回 0。应得金额：
     * <ul>
     *     <li>没有生效过订单变更：round2(行金额 × 已出入库数量 / 订单数量)，行金额已扣除整单优惠的分摊；</li>
     *     <li>生效过订单变更（未出入库部分按变更后的单价重新计价、已出入库部分不变）：行金额 − round2(未出入库数量 × 单价)。</li>
     * </ul>
     * 退款的退货另计（不影响已出入库金额），补发的退货已从已出入库数量和金额中扣除
     *
     * @param item     订单行（关闭时加锁读取的最新值）
     * @param repriced 订单生效过变更（版本 &gt; 1）
     * @return 调整金额（订单币种），正数增加应收 / 应付，负数冲减
     */
    public static BigDecimal closeAdjust(ScmOrderItem item, boolean repriced) {
        if (item.getAmount() == null || item.getQty() == null || item.getQty().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal shipped = shippedQty(item);
        if (shipped.signum() <= 0 || shipped.compareTo(item.getQty()) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal expected = repriced ? item.getAmount().subtract(amount(item.getQty().subtract(shipped), item.getPrice()))
                : item.getAmount().multiply(shipped).divide(item.getQty(), 2, RoundingMode.HALF_UP);
        return expected.subtract(Decimals.nz(item.getDoneAmount())).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 本次出入库（非退货）消耗的整单优惠分摊额：剩余分摊额 × 本次数量 / 未完成数量，完成订单行的一批取剩余
     *
     * @param item 订单行，回写数量之后重新读取的值（同 {@link #outbound}）
     */
    public static BigDecimal outboundShare(ScmOrderItem item, BigDecimal qty) {
        BigDecimal rest = Decimals.nz(item.getDiscountShare()).subtract(Decimals.nz(item.getDoneShare()));
        BigDecimal remaining = item.getQty().subtract(item.getDoneQty().subtract(qty)).add(Decimals.nz(item.getReplenishQty()));
        if (rest.signum() <= 0 || remaining.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return qty.compareTo(remaining) >= 0 ? rest : rest.multiply(qty).divide(remaining, 2, RoundingMode.HALF_UP).min(rest);
    }

    /**
     * 补发的退货退回的分摊额（负数）：已消耗的分摊额 × 本次数量 / 退货前已出入库数量。退款的退货不退回（优惠已经实现）
     *
     * @param item 订单行，回写退货数量之后重新读取的值：replenishQty 已含本次数量
     */
    public static BigDecimal replenishShare(ScmOrderItem item, BigDecimal qty) {
        BigDecimal done = Decimals.nz(item.getDoneShare());
        BigDecimal shippedBefore = shippedQty(item).add(qty);
        if (done.signum() <= 0 || shippedBefore.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return (qty.compareTo(shippedBefore) >= 0 ? done : done.multiply(qty).divide(shippedBefore, 2, RoundingMode.HALF_UP).min(done)).negate();
    }

    /**
     * 退货单本行对应的整单优惠（含税，只用于显示）：按优惠前单价计的金额 − 实际退货金额，不小于 0；
     * 订单行没有分摊整单优惠时为 0。退货金额按扣除优惠后的成交价计算（见 {@link #returned}），所以退回的正是原单的优惠分摊
     *
     * @param item   订单行
     * @param qty    本次退货数量
     * @param amount 本次退货金额（订单币种）
     */
    public static BigDecimal returnShare(ScmOrderItem item, BigDecimal qty, BigDecimal amount) {
        if (Decimals.nz(item.getDiscountShare()).signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = amount(qty, grossPrice(item)).subtract(amount);
        return diff.signum() < 0 ? BigDecimal.ZERO : diff;
    }

    /**
     * 订单变更后的一行
     *
     * @param qty           变更后的数量
     * @param grossPrice    变更后的优惠前单价（没有整单优惠时即单价）
     * @param openQty       变更后未出入库的数量
     * @param doneAmount    已出入库金额
     * @param consumedShare 已出入库部分已分摊的整单优惠
     * @param keep          保持原金额、原分摊额不变（没有影响金额的变更，或已出完且没改的行）
     * @param keepAmount    keep 时的行金额
     * @param keepShare     keep 时的分摊额
     */
    public record ChangeLine(BigDecimal qty, BigDecimal grossPrice, BigDecimal openQty, BigDecimal doneAmount, BigDecimal consumedShare,
                             boolean keep, BigDecimal keepAmount, BigDecimal keepShare) {

        /** 新增的行 */
        public static ChangeLine added(BigDecimal qty, BigDecimal price) {
            return new ChangeLine(qty, price, qty, BigDecimal.ZERO, BigDecimal.ZERO, false, null, null);
        }

        /** 保持不变的行 */
        public static ChangeLine kept(BigDecimal amount, BigDecimal share) {
            return new ChangeLine(null, null, null, null, null, true, amount, Decimals.nz(share));
        }
    }

    /**
     * 变更后的行金额
     *
     * @param price 单价：未出入库部分的成交单价（4 位小数），keep 的行为 null（不变）
     */
    public record LineResult(BigDecimal amount, BigDecimal share, BigDecimal price) {
    }

    /**
     * 订单变更：已出入库的部分不变，未出入库部分 = 未出入库数量 × 优惠前单价，剩余的整单优惠（优惠 − 已出入库部分已分摊的 − 不变行的）
     * 按未出入库部分的金额比例分摊
     *
     * @param discount 变更后的整单优惠
     */
    public static List<LineResult> reallocate(List<ChangeLine> lines, BigDecimal discount) {
        BigDecimal off = Decimals.money(discount);
        if (off.signum() < 0) {
            throw new BizException("scm.error.wholeDiscountNegative");
        }
        BigDecimal fixed = BigDecimal.ZERO;
        List<BigDecimal> open = new ArrayList<>();
        for (ChangeLine l : lines) {
            if (l.keep()) {
                fixed = fixed.add(l.keepShare());
            } else {
                fixed = fixed.add(Decimals.nz(l.consumedShare()));
                open.add(amount(l.openQty(), l.grossPrice()));
            }
        }
        BigDecimal pool = off.subtract(fixed);
        if (pool.signum() < 0) {
            throw new BizException("scm.error.wholeDiscountBelowDone", plain(fixed));
        }
        BigDecimal openTotal = Decimals.sum(open, a -> a);
        if (pool.compareTo(openTotal) > 0) {
            throw new BizException("scm.error.wholeDiscountExceedsOpen", plain(fixed.add(openTotal)));
        }
        List<BigDecimal> net = WholeDiscounts.allocate(open, pool);
        List<LineResult> result = new ArrayList<>(lines.size());
        int k = 0;
        for (ChangeLine l : lines) {
            if (l.keep()) {
                result.add(new LineResult(l.keepAmount(), l.keepShare(), null));
                continue;
            }
            BigDecimal gross = open.get(k);
            BigDecimal lineNet = net.get(k++);
            BigDecimal share = Decimals.nz(l.consumedShare()).add(gross.subtract(lineNet));
            BigDecimal amount = Decimals.nz(l.doneAmount()).add(lineNet);
            BigDecimal price = lineNet.compareTo(gross) == 0 || l.openQty().signum() == 0 ? l.grossPrice()
                    : lineNet.divide(l.openQty(), 4, RoundingMode.HALF_UP);
            result.add(new LineResult(amount, share, price));
        }
        return result;
    }

    /**
     * 已出入库部分已分摊的整单优惠（出入库时累计的 doneShare，不超过分摊额）；没有未完成数量时为全部分摊额
     *
     * @param item 变更前的订单行
     */
    public static BigDecimal consumedShare(ScmOrderItem item) {
        BigDecimal share = Decimals.nz(item.getDiscountShare());
        if (share.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (openQty(item, item.getQty()).signum() == 0) {
            return share;
        }
        BigDecimal done = Decimals.nz(item.getDoneShare());
        return done.signum() < 0 ? BigDecimal.ZERO : done.min(share);
    }

    /** 优惠前单价：行金额加回分摊的整单优惠后除以数量；没有分摊时即单价 */
    public static BigDecimal grossPrice(ScmOrderItem item) {
        BigDecimal share = Decimals.nz(item.getDiscountShare());
        if (share.signum() == 0 || item.getQty().signum() == 0) {
            return item.getPrice();
        }
        return item.getAmount().add(share).divide(item.getQty(), 4, RoundingMode.HALF_UP);
    }

    /** 已出入库数量（已完成 − 需要补发的退货） */
    public static BigDecimal shippedQty(ScmOrderItem item) {
        BigDecimal shipped = item.getDoneQty().subtract(Decimals.nz(item.getReplenishQty()));
        return shipped.signum() < 0 ? BigDecimal.ZERO : shipped;
    }

    /** 变更后未出入库的数量 */
    public static BigDecimal openQty(ScmOrderItem item, BigDecimal newQty) {
        BigDecimal open = newQty.subtract(shippedQty(item));
        return open.signum() < 0 ? BigDecimal.ZERO : open;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
