package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.entity.ScmSupplierPriceTier;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceTierMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 供应链对其他模块提供的只读查询（生产计划、MRP、质量检验）
 */
@Service
@RequiredArgsConstructor
public class ScmQueryService {

    private final ScmOrderMapper orderMapper;
    private final ScmSupplierPriceMapper priceMapper;
    private final ScmTradeDocMapper tradeDocMapper;
    private final ScmTradeDocItemMapper tradeDocItemMapper;
    private final ScmSupplierPriceTierMapper tierMapper;
    private final ScmPurchaseContractMapper contractMapper;

    /** 采购在途：未结束采购订单的未入库数量，按物料 */
    public Map<Long, BigDecimal> openPurchaseQty(Collection<Long> materialIds) {
        return openQty(OrderKind.PURCHASE, materialIds);
    }

    /** 销售待发：未结束销售订单的未出库数量，按物料 */
    public Map<Long, BigDecimal> openSalesQty(Collection<Long> materialIds) {
        return openQty(OrderKind.SALES, materialIds);
    }

    /** 物料的默认供应商价格 */
    public Map<Long, ScmSupplierPrice> defaultSuppliers(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return priceMapper.selectDefaults(materialIds).stream()
                .collect(Collectors.toMap(ScmSupplierPrice::getMaterialId, Function.identity(), (a, b) -> a));
    }

    /**
     * 按采购数量取阶梯价：数量达到的最高一档阶梯，没有阶梯或数量不足时取基础单价
     */
    public BigDecimal effectivePrice(ScmSupplierPrice price, BigDecimal qty) {
        if (price == null) {
            return null;
        }
        if (qty == null) {
            return price.getPrice();
        }
        return tierMapper.selectList(Wrappers.<ScmSupplierPriceTier>lambdaQuery().eq(ScmSupplierPriceTier::getPriceId, price.getId())
                        .le(ScmSupplierPriceTier::getMinQty, qty).orderByDesc(ScmSupplierPriceTier::getMinQty))
                .stream().findFirst().map(ScmSupplierPriceTier::getPrice).orElse(price.getPrice());
    }

    /** 供应商对某物料当前有效的供货价格，没有或不在有效期内时返回 null */
    public ScmSupplierPrice supplierPrice(Long supplierId, Long materialId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return priceMapper.selectOne(Wrappers.<ScmSupplierPrice>lambdaQuery()
                .eq(ScmSupplierPrice::getSupplierId, supplierId)
                .eq(ScmSupplierPrice::getMaterialId, materialId)
                .and(w -> w.isNull(ScmSupplierPrice::getValidFrom).or().le(ScmSupplierPrice::getValidFrom, today))
                .and(w -> w.isNull(ScmSupplierPrice::getValidTo).or().ge(ScmSupplierPrice::getValidTo, today)));
    }

    /** 采购入库 / 退货单的订单引用了采购合同且合同约定了付款天数时返回该天数，否则 null（应付单按供应商账期） */
    public Integer contractPaymentDays(Long tradeDocId) {
        return contractMapper.selectPaymentDaysByTradeDoc(tradeDocId);
    }

    public String orderNo(Long orderId) {
        ScmOrder order = orderMapper.selectById(orderId);
        return order == null ? null : order.getOrderNo();
    }

    /** 采购入库单号对应的供应商，单据不存在时返回 null（来料检验带出供应商） */
    /**
     * 已审核及以后的销售订单（建项目），不存在或不是销售订单返回 null
     *
     * @return {orderNo, partnerId, totalAmount}
     */
    public Map<String, Object> salesOrder(Long orderId) {
        ScmOrder o = orderId == null ? null : orderMapper.selectById(orderId);
        if (o == null || o.getKind() != OrderKind.SALES || o.getStatus().isUnaudited()) {
            return null;
        }
        return Map.of("orderNo", o.getOrderNo(), "partnerId", o.getPartnerId(), "totalAmount", o.getTotalAmount());
    }

    public Long purchaseInSupplier(String docNo) {
        ScmTradeDoc doc = tradeDocMapper.selectOne(Wrappers.<ScmTradeDoc>lambdaQuery()
                .eq(ScmTradeDoc::getDocNo, docNo)
                .eq(ScmTradeDoc::getKind, TradeKind.PURCHASE_IN));
        return doc == null ? null : doc.getPartnerId();
    }

    /** 采购入库单中某物料的入库数量（来料检验校验送检数量），单据或物料不存在时为 0 */
    public BigDecimal purchaseInQty(String docNo, Long materialId) {
        return Decimals.nz(tradeDocItemMapper.sumQtyByDocNo(docNo, TradeKind.PURCHASE_IN, materialId));
    }

    private Map<Long, BigDecimal> openQty(OrderKind kind, Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : orderMapper.selectOpenQty(kind, OrderStatus.OPEN, materialIds)) {
            map.put(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        return map;
    }
}
