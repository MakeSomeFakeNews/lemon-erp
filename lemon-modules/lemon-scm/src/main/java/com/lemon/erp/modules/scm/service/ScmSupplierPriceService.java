package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.scm.dto.SupplierPriceRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.entity.ScmSupplierPriceTier;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceTierMapper;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供货价格：供应商可供应的物料、含税单价、提前期；每个物料可以指定一个默认供应商
 */
@Service
@RequiredArgsConstructor
public class ScmSupplierPriceService {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmSupplierPriceMapper priceMapper;
    private final ScmSupplierService supplierService;
    private final WmsMaterialService materialService;
    private final ScmSupplierPriceTierMapper tierMapper;

    public PageResult<ScmSupplierPrice> page(long current, long size, Long supplierId, Long materialId, String keyword) {
        Page<ScmSupplierPrice> page = Page.of(current, size);
        priceMapper.selectPageList(page, supplierId, materialId, keyword);
        return PageResult.of(page);
    }

    /** 供应商当前有效的供货价格，采购订单选择物料后带出单价、税率 */
    public List<ScmSupplierPrice> listBySupplier(Long supplierId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return priceMapper.selectList(Wrappers.<ScmSupplierPrice>lambdaQuery().eq(ScmSupplierPrice::getSupplierId, supplierId)
                .and(w -> w.isNull(ScmSupplierPrice::getValidFrom).or().le(ScmSupplierPrice::getValidFrom, today))
                .and(w -> w.isNull(ScmSupplierPrice::getValidTo).or().ge(ScmSupplierPrice::getValidTo, today)));
    }

    @Transactional
    public void create(SupplierPriceRequest req) {
        supplierService.get(req.supplierId());
        materialService.get(req.materialId());
        ScmSupplierPrice price = toEntity(req);
        try {
            priceMapper.insert(price);
        } catch (DuplicateKeyException e) {
            throw new BizException("该供应商已有此物料的供货价格，请直接修改");
        }
        saveTiers(price.getId(), req);
        if (Integer.valueOf(1).equals(price.getIsDefault())) {
            clearOtherDefaults(price);
        }
    }

    /** 供应商和物料不能修改 */
    @Transactional
    public void update(Long id, SupplierPriceRequest req) {
        ScmSupplierPrice old = get(id);
        ScmSupplierPrice price = toEntity(req);
        price.setId(id);
        price.setSupplierId(old.getSupplierId());
        price.setMaterialId(old.getMaterialId());
        priceMapper.updateById(price);
        priceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate().set(ScmSupplierPrice::getValidFrom, req.validFrom())
                .set(ScmSupplierPrice::getValidTo, req.validTo()).eq(ScmSupplierPrice::getId, id));
        saveTiers(id, req);
        if (req.minQty() == null && old.getMinQty() != null) {
            priceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate().set(ScmSupplierPrice::getMinQty, null).eq(ScmSupplierPrice::getId, id));
        }
        if (Integer.valueOf(1).equals(price.getIsDefault())) {
            clearOtherDefaults(price);
        }
    }

    @Transactional
    public void delete(List<Long> ids) {
        priceMapper.deleteByIds(ids);
        tierMapper.delete(Wrappers.<ScmSupplierPriceTier>lambdaQuery().in(ScmSupplierPriceTier::getPriceId, ids));
    }

    /**
     * 询价定标后更新供货价格：已有价格只更新单价、税率、提前期（阶梯价保留），没有时新建
     */
    @Transactional
    public void applyQuote(Long supplierId, Long materialId, BigDecimal price, BigDecimal taxRate, Integer leadDays) {
        ScmSupplierPrice old = priceMapper.selectOne(Wrappers.<ScmSupplierPrice>lambdaQuery()
                .eq(ScmSupplierPrice::getSupplierId, supplierId).eq(ScmSupplierPrice::getMaterialId, materialId));
        if (old == null) {
            ScmSupplierPrice p = new ScmSupplierPrice();
            p.setSupplierId(supplierId);
            p.setMaterialId(materialId);
            p.setPrice(price);
            p.setTaxRate(taxRate == null ? DEFAULT_TAX_RATE : taxRate);
            p.setLeadDays(leadDays);
            boolean hasDefault = priceMapper.exists(Wrappers.<ScmSupplierPrice>lambdaQuery()
                    .eq(ScmSupplierPrice::getMaterialId, materialId).eq(ScmSupplierPrice::getIsDefault, 1));
            p.setIsDefault(hasDefault ? 0 : 1);
            p.setRemark("询价定标");
            priceMapper.insert(p);
            return;
        }
        priceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate().set(ScmSupplierPrice::getPrice, price)
                .set(taxRate != null, ScmSupplierPrice::getTaxRate, taxRate)
                .set(leadDays != null, ScmSupplierPrice::getLeadDays, leadDays)
                .eq(ScmSupplierPrice::getId, old.getId()));
    }

    /** 阶梯价（按起订数量从小到大） */
    public List<ScmSupplierPriceTier> tiers(Long priceId) {
        return tierMapper.selectList(Wrappers.<ScmSupplierPriceTier>lambdaQuery().eq(ScmSupplierPriceTier::getPriceId, priceId)
                .orderByAsc(ScmSupplierPriceTier::getMinQty));
    }

    /** 采购价格历史：采购入库的单价 */
    public List<java.util.Map<String, Object>> history(Long materialId, Long supplierId) {
        return priceMapper.selectPurchaseHistory(materialId, supplierId);
    }

    private void saveTiers(Long priceId, SupplierPriceRequest req) {
        tierMapper.delete(Wrappers.<ScmSupplierPriceTier>lambdaQuery().eq(ScmSupplierPriceTier::getPriceId, priceId));
        if (req.tiers() == null) {
            return;
        }
        if (req.validFrom() != null && req.validTo() != null && req.validTo().isBefore(req.validFrom())) {
            throw new BizException("有效期止不能早于有效期起");
        }
        java.util.Set<BigDecimal> seen = new java.util.HashSet<>();
        for (SupplierPriceRequest.Tier t : req.tiers()) {
            if (!seen.add(t.minQty().stripTrailingZeros())) {
                throw new BizException("阶梯起订数量重复：" + t.minQty().stripTrailingZeros().toPlainString());
            }
            ScmSupplierPriceTier tier = new ScmSupplierPriceTier();
            tier.setPriceId(priceId);
            tier.setMinQty(t.minQty());
            tier.setPrice(t.price());
            tierMapper.insert(tier);
        }
    }

    private ScmSupplierPrice get(Long id) {
        ScmSupplierPrice price = priceMapper.selectById(id);
        if (price == null) {
            throw new BizException("供货价格不存在");
        }
        return price;
    }

    /** 同一物料只有一个默认供应商 */
    private void clearOtherDefaults(ScmSupplierPrice price) {
        priceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate()
                .set(ScmSupplierPrice::getIsDefault, 0)
                .eq(ScmSupplierPrice::getMaterialId, price.getMaterialId())
                .ne(ScmSupplierPrice::getId, price.getId())
                .eq(ScmSupplierPrice::getIsDefault, 1));
    }

    private static ScmSupplierPrice toEntity(SupplierPriceRequest req) {
        ScmSupplierPrice price = new ScmSupplierPrice();
        price.setSupplierId(req.supplierId());
        price.setMaterialId(req.materialId());
        price.setPrice(req.price());
        price.setTaxRate(req.taxRate() == null ? DEFAULT_TAX_RATE : req.taxRate());
        price.setLeadDays(req.leadDays() == null ? 0 : req.leadDays());
        price.setMinQty(req.minQty());
        price.setIsDefault(Integer.valueOf(1).equals(req.isDefault()) ? 1 : 0);
        price.setRemark(req.remark());
        price.setValidFrom(req.validFrom());
        price.setValidTo(req.validTo());
        return price;
    }
}
