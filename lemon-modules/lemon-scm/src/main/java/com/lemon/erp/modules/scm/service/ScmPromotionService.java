package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.dto.PromotionRequests;
import com.lemon.erp.modules.scm.dto.ScmPriceViews;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 促销活动：手工新建 / 修改销售订单时按订单日期、客户等级自动应用，计算规则见 {@link PromotionCalculator}
 */
@Service
@RequiredArgsConstructor
public class ScmPromotionService {

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
            throw new BizException("scm.error.promotionDoesNotExist");
        }
        return p;
    }

    public List<ScmPriceViews.PromotionUsage> usages(Long id) {
        detail(id);
        return promotionMapper.selectUsages(id);
    }

    @Transactional
    public Long create(PromotionRequests.Save req) {
        ScmPromotion p = new ScmPromotion();
        fill(p, req);
        p.setStatus(EnableStatus.ENABLED);
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
            throw new BizException("scm.error.promotionHasAppliedOrdersCannotDeleted", p.getName());
        }
        promotionMapper.deleteById(id);
    }

    public void changeStatus(Long id, EnableStatus status) {
        ScmPromotion p = detail(id);
        p.setStatus(status);
        promotionMapper.updateById(p);
    }

    /** 下单前预览：每行的成交价和促销、赠品、优惠合计 */
    public ScmPriceViews.PromotionPreview preview(ScmCustomer customer, PromotionRequests.Preview req) {
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
        List<ScmPriceViews.PreviewLine> lines = new ArrayList<>();
        List<ScmPriceViews.PreviewGift> gifts = new ArrayList<>();
        for (ScmOrderItem i : a.items()) {
            String promotionName = i.getPromotionId() == null ? null : names.get(i.getPromotionId());
            if (Integer.valueOf(1).equals(i.getGift())) {
                WmsMaterial m = materials.get(i.getMaterialId());
                gifts.add(new ScmPriceViews.PreviewGift(i.getMaterialId(), i.getPrice(), i.getOrigPrice(), promotionName,
                        m == null ? null : m.getCode() + " " + m.getName(), i.getQty()));
            } else {
                lines.add(new ScmPriceViews.PreviewLine(i.getMaterialId(), i.getPrice(), i.getOrigPrice(), promotionName));
            }
        }
        return new ScmPriceViews.PromotionPreview(lines, gifts, a.discountAmount(), a.notes());
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
        return PromotionCalculator.calculate(promos, materials, items, rebate);
    }

    private void fill(ScmPromotion p, PromotionRequests.Save req) {
        if (req.endDate().isBefore(req.beginDate())) {
            throw new BizException("scm.error.endDateCannotEarlierStartDate");
        }
        switch (req.promoType()) {
            case DISCOUNT -> {
                if (req.discountRate() == null || req.discountRate().signum() <= 0 || req.discountRate().compareTo(Decimals.HUNDRED) >= 0) {
                    throw new BizException("scm.error.discountRateMustGreaterLess");
                }
            }
            case REDUCE -> {
                if (req.thresholdAmount() == null || req.reduceAmount() == null || req.reduceAmount().signum() <= 0
                        || req.reduceAmount().compareTo(req.thresholdAmount()) >= 0) {
                    throw new BizException("scm.error.enterThresholdReductionAmountReductionMust");
                }
            }
            case GIFT -> {
                if (req.buyQty() == null || req.buyQty().signum() <= 0 || req.giftMaterialId() == null || req.giftQty() == null
                        || req.giftQty().signum() <= 0) {
                    throw new BizException("scm.error.enterPurchaseQuantityGiftGiftQuantity");
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
}
