package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.scm.dto.PromotionRequests;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.entity.ScmPromotion;
import com.lemon.erp.modules.scm.mapper.ScmPromotionMapper;
import com.lemon.erp.modules.scm.model.PromoType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;

/**
 * 促销活动：手工新建 / 修改销售订单时按订单日期、客户等级自动应用。
 * 顺序：每行取适用的最低折扣 → 适用物料（折扣后）金额达到门槛的满减取减免最多的一个，按行金额分摊到单价 → 买赠按适用物料数量生成赠品行
 * → 返利抵扣按非赠品行金额分摊到单价。原价记入 origPrice，每个促销的优惠金额、赠品数量记入应用记录
 */
@Service
@RequiredArgsConstructor
public class ScmPromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal GIFT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmPromotionMapper promotionMapper;
    private final WmsMaterialService materialService;

    /** 应用结果：明细（含赠品行，单价已调整、金额未重算）、促销优惠合计、各促销的应用记录、说明 */
    public record Applied(List<ScmOrderItem> items, BigDecimal discountAmount, List<Usage> usages, List<String> notes) {
    }

    public record Usage(Long promotionId, String name, BigDecimal discountAmount, BigDecimal giftQty) {
    }

    public PageResult<ScmPromotion> page(long current, long size, String keyword, PromoType promoType, LocalDate activeOn) {
        Page<ScmPromotion> page = Page.of(current, size);
        promotionMapper.selectPageList(page, keyword, promoType, activeOn);
        return PageResult.of(page);
    }

    public ScmPromotion detail(Long id) {
        ScmPromotion p = id == null ? null : promotionMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("促销活动不存在");
        }
        return p;
    }

    public List<Map<String, Object>> usages(Long id) {
        detail(id);
        return promotionMapper.selectUsages(id);
    }

    @Transactional
    public Long create(PromotionRequests.Save req) {
        ScmPromotion p = new ScmPromotion();
        fill(p, req);
        p.setStatus(1);
        p.setCreateBy(SecurityUtils.getUserId());
        promotionMapper.insert(p);
        return p.getId();
    }

    @Transactional
    public void update(Long id, PromotionRequests.Save req) {
        ScmPromotion p = detail(id);
        fill(p, req);
        promotionMapper.updateById(p);
    }

    /** 已应用到订单的不能删除，可以停用 */
    @Transactional
    public void delete(Long id) {
        ScmPromotion p = detail(id);
        if (p.getOrderCount() != null && p.getOrderCount() > 0) {
            throw new BizException("促销「" + p.getName() + "」已应用到订单，不能删除，可以停用");
        }
        promotionMapper.deleteById(id);
    }

    public void changeStatus(Long id, int status) {
        ScmPromotion p = detail(id);
        p.setStatus(status == 1 ? 1 : 0);
        promotionMapper.updateById(p);
    }

    /** 下单前预览：每行的成交价和促销、赠品、优惠合计 */
    public Map<String, Object> preview(ScmCustomer customer, PromotionRequests.Preview req) {
        List<ScmOrderItem> items = req.items().stream().map(l -> {
            ScmOrderItem i = new ScmOrderItem();
            i.setMaterialId(l.materialId());
            i.setQty(l.qty());
            i.setPrice(l.price());
            return i;
        }).toList();
        Applied a = apply(customer, req.orderDate(), items, req.rebateAmount());
        Map<Long, String> names = new HashMap<>();
        a.usages().forEach(u -> names.put(u.promotionId(), u.name()));
        Map<Long, WmsMaterial> materials = materialService.mapByIds(a.items().stream().map(ScmOrderItem::getMaterialId).toList());
        List<Map<String, Object>> lines = new ArrayList<>();
        List<Map<String, Object>> gifts = new ArrayList<>();
        for (ScmOrderItem i : a.items()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", i.getMaterialId());
            row.put("price", i.getPrice());
            row.put("origPrice", i.getOrigPrice());
            row.put("promotionName", i.getPromotionId() == null ? null : names.get(i.getPromotionId()));
            if (Integer.valueOf(1).equals(i.getGift())) {
                WmsMaterial m = materials.get(i.getMaterialId());
                row.put("materialName", m == null ? null : m.getCode() + " " + m.getName());
                row.put("qty", i.getQty());
                gifts.add(row);
            } else {
                lines.add(row);
            }
        }
        return Map.of("lines", lines, "gifts", gifts, "discountAmount", a.discountAmount(), "notes", a.notes());
    }

    /** 订单保存后记录应用的促销（修改时先删除旧记录） */
    public void saveUsages(Long orderId, List<Usage> usages) {
        promotionMapper.deleteUsages(orderId);
        if (!usages.isEmpty()) {
            promotionMapper.insertUsages(orderId, usages.stream().map(u -> Map.<String, Object>of("promotionId", u.promotionId(),
                    "discountAmount", u.discountAmount(), "giftQty", u.giftQty())).toList());
        }
    }

    public void deleteUsages(Long orderId) {
        promotionMapper.deleteUsages(orderId);
    }

    /**
     * 应用促销和返利抵扣
     *
     * @param items  订单明细，单价为原价
     * @param rebate 返利抵扣金额，为空或 0 不抵扣
     */
    public Applied apply(ScmCustomer customer, LocalDate orderDate, List<ScmOrderItem> items, BigDecimal rebate) {
        List<ScmPromotion> promos = promotionMapper.selectApplicable(orderDate, customer.getLevel());
        Set<Long> ids = new HashSet<>();
        items.forEach(i -> ids.add(i.getMaterialId()));
        promos.stream().map(ScmPromotion::getGiftMaterialId).filter(Objects::nonNull).forEach(ids::add);
        Map<Long, WmsMaterial> materials = ids.isEmpty() ? Map.of() : materialService.mapByIds(ids);
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
                        item.setPrice(item.getPrice().multiply(p.getDiscountRate()).divide(HUNDRED, 4, RoundingMode.HALF_UP));
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
            notes.add("满 " + plain(bestReduce.getThresholdAmount()) + " 减 " + plain(bestReduce.getReduceAmount()) + "（" + bestReduce.getName() + "）");
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
            g.setRemark("赠品：" + p.getName());
            result.add(g);
            usage(usages, p, BigDecimal.ZERO, g.getQty());
            notes.add("赠 " + gift.getName() + " × " + plain(g.getQty()) + "（" + p.getName() + "）");
        }
        BigDecimal discount = usages.values().stream().map(Usage::discountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 返利抵扣：按非赠品行金额分摊
        if (rebate != null && rebate.signum() > 0) {
            if (rebate.compareTo(sum(items)) > 0) {
                throw new BizException("返利抵扣金额不能超过订单金额 " + plain(sum(items)));
            }
            allocate(items, rebate);
            notes.add("返利抵扣 " + plain(rebate));
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
        return lines.stream().map(i -> amount(i.getQty(), i.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private void fill(ScmPromotion p, PromotionRequests.Save req) {
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        switch (req.promoType()) {
            case DISCOUNT -> {
                if (req.discountRate() == null || req.discountRate().signum() <= 0 || req.discountRate().compareTo(HUNDRED) >= 0) {
                    throw new BizException("折扣率须大于 0、小于 100");
                }
            }
            case REDUCE -> {
                if (req.thresholdAmount() == null || req.reduceAmount() == null || req.reduceAmount().signum() <= 0
                        || req.reduceAmount().compareTo(req.thresholdAmount()) >= 0) {
                    throw new BizException("请填写满减门槛和减免金额，减免金额须小于门槛");
                }
            }
            case GIFT -> {
                if (req.buyQty() == null || req.buyQty().signum() <= 0 || req.giftMaterialId() == null || req.giftQty() == null
                        || req.giftQty().signum() <= 0) {
                    throw new BizException("请填写购买数量、赠品和赠送数量");
                }
                materialService.getEnabled(List.of(req.giftMaterialId()));
            }
        }
        if (req.materialId() != null) {
            materialService.getEnabled(List.of(req.materialId()));
        }
        p.setName(req.name().trim());
        p.setPromoType(req.promoType());
        p.setBeginDate(req.beginDate());
        p.setEndDate(req.endDate());
        p.setCustomerLevel(blank(req.customerLevel()));
        p.setMaterialId(req.materialId());
        p.setMaterialCategory(req.materialId() != null ? null : blank(req.materialCategory()));
        boolean discount = req.promoType() == PromoType.DISCOUNT;
        boolean reduce = req.promoType() == PromoType.REDUCE;
        boolean gift = req.promoType() == PromoType.GIFT;
        p.setMinQty(discount ? req.minQty() : null);
        p.setDiscountRate(discount ? req.discountRate() : null);
        p.setThresholdAmount(reduce ? req.thresholdAmount() : null);
        p.setReduceAmount(reduce ? req.reduceAmount() : null);
        p.setBuyQty(gift ? req.buyQty() : null);
        p.setGiftMaterialId(gift ? req.giftMaterialId() : null);
        p.setGiftQty(gift ? req.giftQty() : null);
        p.setRemark(req.remark());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
