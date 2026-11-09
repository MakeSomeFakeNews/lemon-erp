package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.event.OrderCloseAdjustedEvent;
import com.lemon.erp.modules.scm.mapper.ScmOrderItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocItemMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.StockCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单关闭时的尾差调整（见 {@link OrderLineAmounts#closeAdjust}）：在关闭的事务中按行计算差额，计入已出入库金额
 * （之后的退货按调整后的金额计算）和 close_adjust，发布 {@link OrderCloseAdjustedEvent} 由财务生成应收 / 应付调整单；
 * 采购订单的差额（不含税，本位币）同时追加到该行最近一张采购入库单的实时库存成本（物料已没有库存时不追加）
 */
@Component
@RequiredArgsConstructor
public class ScmOrderCloseAdjuster {

    private final ScmOrderMapper orderMapper;
    private final ScmOrderItemMapper itemMapper;
    private final ScmTradeDocItemMapper tradeItemMapper;
    private final StockCostService stockCostService;
    private final ApplicationEventPublisher eventPublisher;

    /** @param order 已锁定并改为已关闭的订单 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void adjust(ScmOrder order) {
        BigDecimal rate = order.getExchangeRate() == null ? BigDecimal.ONE : order.getExchangeRate();
        boolean repriced = order.getVersion() != null && order.getVersion() > 1;
        BigDecimal totalFc = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        Map<Long, BigDecimal> netByItem = new LinkedHashMap<>();
        Map<Long, Long> materialByItem = new HashMap<>();
        for (ScmOrderItem item : itemMapper.selectForClose(order.getId())) {
            BigDecimal diff = OrderLineAmounts.closeAdjust(item, repriced);
            if (diff.signum() == 0) {
                continue;
            }
            BigDecimal base = rate.compareTo(BigDecimal.ONE) == 0 ? diff : diff.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal t = tax(base, item.getTaxRate());
            itemMapper.addCloseAdjust(item.getId(), diff, base.subtract(t));
            totalFc = totalFc.add(diff);
            total = total.add(base);
            totalTax = totalTax.add(t);
            netByItem.put(item.getId(), base.subtract(t));
            materialByItem.put(item.getId(), item.getMaterialId());
        }
        if (netByItem.isEmpty()) {
            return;
        }
        orderMapper.update(Wrappers.<ScmOrder>lambdaUpdate().set(ScmOrder::getCloseAdjust, totalFc)
                .set(ScmOrder::getUpdateTime, LocalDateTime.now()).eq(ScmOrder::getId, order.getId()));
        if (order.getKind() == OrderKind.PURCHASE) {
            addPurchaseCost(netByItem, materialByItem);
        }
        if (total.signum() != 0) {
            eventPublisher.publishEvent(new OrderCloseAdjustedEvent(order.getKind(), order.getId(), order.getOrderNo(), order.getPartnerId(),
                    LocalDate.now(), total, totalTax, order.getCurrency() == null ? ExchangeRateProvider.BASE : order.getCurrency(), rate, totalFc));
        }
    }

    /** 采购的尾差（不含税）追加到各行最近一张采购入库单的成本：同一入库单同一物料合并 */
    private void addPurchaseCost(Map<Long, BigDecimal> netByItem, Map<Long, Long> materialByItem) {
        Map<Long, Map<Long, BigDecimal>> byDoc = new LinkedHashMap<>();
        for (Map<String, Object> row : tradeItemMapper.selectLastWmsDocs(netByItem.keySet(), TradeKind.PURCHASE_IN)) {
            Long itemId = ((Number) row.get("orderItemId")).longValue();
            Long docId = ((Number) row.get("wmsDocId")).longValue();
            byDoc.computeIfAbsent(docId, k -> new HashMap<>()).merge(materialByItem.get(itemId), netByItem.get(itemId), BigDecimal::add);
        }
        byDoc.forEach((docId, extra) -> stockCostService.addCost(StockBizType.IN, docId, extra));
    }
}
