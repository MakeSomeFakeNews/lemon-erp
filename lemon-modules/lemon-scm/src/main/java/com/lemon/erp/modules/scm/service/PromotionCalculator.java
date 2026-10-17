package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmPromotion;
import com.lemon.erp.modules.scm.model.PromoType;
import com.lemon.erp.modules.scm.service.ScmPromotionService.Applied;
import com.lemon.erp.modules.scm.service.ScmPromotionService.Usage;
import com.lemon.erp.modules.wms.entity.WmsMaterial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;

/**
 * 促销与返利抵扣的计算（纯计算，不访问数据库）。
 * 顺序：每行取适用的最低折扣 → 适用物料（折扣后）金额达到门槛的满减取减免最多的一个，按行金额分摊到单价 → 买赠按适用物料数量生成赠品行
 * → 返利抵扣按非赠品行金额分摊到单价。原价记入 origPrice，每个促销的优惠金额、赠品数量记入应用记录
 */
public final class PromotionCalculator {

    private static final BigDecimal GIFT_TAX_RATE = BigDecimal.valueOf(13);

    private PromotionCalculator() {
    }

    /**
     * 应用促销和返利抵扣：会修改 items 的单价、促销、原价
     *
     * @param promos    适用的促销活动（已按订单日期、客户等级筛选）
     * @param materials 明细物料和赠品物料
     * @param items     订单明细，单价为原价
     * @param rebate    返利抵扣金额，为空或 0 不抵扣
     * @return 明细（含赠品行，单价已调整、金额未重算）、促销优惠合计、各促销的应用记录、说明
     */
    public static Applied calculate(List<ScmPromotion> promos, Map<Long, WmsMaterial> materials, List<ScmOrderItem> items,
                                    BigDecimal rebate) {
        Map<Long, Usage> usages = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        List<ScmOrderItem> result = new ArrayList<>(items);
        // 明细是可变的 @Data 对象（hashCode 随单价变化），按引用记录原价
        Map<ScmOrderItem, BigDecimal> origPrices = new IdentityHashMap<>();
        items.forEach(i -> origPrices.put(i, i.getPrice()));

        // 折扣：每行取适用的最低折扣率
        for (ScmOrderItem item : items) {
            WmsMaterial m = materials.get(item.getMaterialId());
            promos.stream().filter(p -> p.getPromoType() == PromoType.DISCOUNT && matches(p, m)
                            && (p.getMinQty() == null || item.getQty().compareTo(p.getMinQty()) >= 0))
                    .min(Comparator.comparing(ScmPromotion::getDiscountRate))
                    .ifPresent(p -> {
                        BigDecimal before = amount(item.getQty(), item.getPrice());
                        item.setPrice(item.getPrice().multiply(p.getDiscountRate()).divide(Decimals.HUNDRED, 4, RoundingMode.HALF_UP));
                        item.setPromotionId(p.getId());
                        usage(usages, p, before.subtract(amount(item.getQty(), item.getPrice())), BigDecimal.ZERO);
                    });
        }
        // 满减：适用物料金额达到门槛的，取减免最多的一个
        ScmPromotion bestReduce = null;
        List<ScmOrderItem> reduceLines = List.of();
        for (ScmPromotion p : promos) {
            if (p.getPromoType() != PromoType.REDUCE) {
                continue;
            }
            List<ScmOrderItem> lines = items.stream().filter(i -> matches(p, materials.get(i.getMaterialId()))).toList();
            BigDecimal total = sum(lines);
            if (total.compareTo(p.getThresholdAmount()) >= 0 && (bestReduce == null || p.getReduceAmount().compareTo(bestReduce.getReduceAmount()) > 0)) {
                bestReduce = p;
                reduceLines = lines;
            }
        }
        if (bestReduce != null) {
            BigDecimal off = allocate(reduceLines, bestReduce.getReduceAmount());
            usage(usages, bestReduce, off, BigDecimal.ZERO);
            notes.add(Messages.get("scm.text.offOver", plain(bestReduce.getThresholdAmount()), plain(bestReduce.getReduceAmount()), bestReduce.getName()));
        }
        // 买赠：每买 buyQty 个适用物料赠送 giftQty 个赠品
        for (ScmPromotion p : promos) {
            if (p.getPromoType() != PromoType.GIFT) {
                continue;
            }
            BigDecimal qty = items.stream().filter(i -> matches(p, materials.get(i.getMaterialId())))
                    .map(ScmOrderItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal times = qty.divide(p.getBuyQty(), 0, RoundingMode.FLOOR);
            WmsMaterial gift = materials.get(p.getGiftMaterialId());
            if (times.signum() <= 0 || gift == null) {
                continue;
            }
            ScmOrderItem g = new ScmOrderItem();
            g.setMaterialId(gift.getId());
            g.setQty(times.multiply(p.getGiftQty()));
            g.setPrice(BigDecimal.ZERO);
            g.setTaxRate(GIFT_TAX_RATE);
            g.setGift(1);
            g.setPromotionId(p.getId());
            g.setDeliveryDate(items.get(0).getDeliveryDate());
            g.setRemark("赠品：" + p.getName()); // i18n-ignore
            result.add(g);
            usage(usages, p, BigDecimal.ZERO, g.getQty());
            notes.add(Messages.get("scm.text.gift", gift.getName(), plain(g.getQty()), p.getName()));
        }
        BigDecimal discount = Decimals.sum(usages.values(), Usage::discountAmount);
        // 返利抵扣：按非赠品行金额分摊
        if (rebate != null && rebate.signum() > 0) {
            if (rebate.compareTo(sum(items)) > 0) {
                throw new BizException("scm.error.rebateDeductionCannotExceedOrderAmount", plain(sum(items)));
            }
            allocate(items, rebate);
            notes.add(Messages.get("scm.text.rebateDeduction", plain(rebate)));
        }
        items.forEach(i -> i.setOrigPrice(i.getPrice().compareTo(origPrices.get(i)) == 0 ? null : origPrices.get(i)));
        return new Applied(result, discount, new ArrayList<>(usages.values()), notes);
    }

    /** 按行金额分摊减免到单价，返回实际减少的金额 */
    private static BigDecimal allocate(List<ScmOrderItem> lines, BigDecimal off) {
        BigDecimal total = sum(lines);
        if (total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal factor = BigDecimal.ONE.subtract(off.divide(total, 10, RoundingMode.HALF_UP));
        for (ScmOrderItem i : lines) {
            i.setPrice(i.getPrice().multiply(factor).setScale(4, RoundingMode.HALF_UP));
        }
        return total.subtract(sum(lines));
    }

    private static BigDecimal sum(List<ScmOrderItem> lines) {
        return Decimals.sum(lines, i -> amount(i.getQty(), i.getPrice()));
    }

    private static boolean matches(ScmPromotion p, WmsMaterial m) {
        if (m == null) {
            return false;
        }
        if (p.getMaterialId() != null) {
            return p.getMaterialId().equals(m.getId());
        }
        return p.getMaterialCategory() == null || p.getMaterialCategory().equals(m.getCategory());
    }

    private static void usage(Map<Long, Usage> usages, ScmPromotion p, BigDecimal discount, BigDecimal giftQty) {
        usages.merge(p.getId(), new Usage(p.getId(), p.getName(), discount, giftQty),
                (a, b) -> new Usage(a.promotionId(), a.name(), a.discountAmount().add(b.discountAmount()), a.giftQty().add(b.giftQty())));
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
