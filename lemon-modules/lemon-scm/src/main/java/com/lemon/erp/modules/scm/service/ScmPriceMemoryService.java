package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.scm.entity.ScmPriceMemory;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.event.TradeDocConfirmedEvent;
import com.lemon.erp.modules.scm.mapper.ScmPriceMemoryMapper;
import com.lemon.erp.modules.scm.mapper.ScmTradeDocItemMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 价格记忆：销售出库、采购入库确认后（同一事务）登记 往来单位 × 物料 的最新 / 最低 / 最高价（含税、基本单位、本位币）。
 * 退货不登记；单价为 0 的行（赠品）不登记
 */
@Service
@RequiredArgsConstructor
public class ScmPriceMemoryService {

    private final ScmPriceMemoryMapper memoryMapper;
    private final ScmTradeDocItemMapper tradeItemMapper;

    @EventListener
    public void onTradeDocConfirmed(TradeDocConfirmedEvent e) {
        if (e.kind() != TradeKind.SALES_OUT && e.kind() != TradeKind.PURCHASE_IN) {
            return;
        }
        List<ScmTradeDocItem> items = tradeItemMapper.selectList(Wrappers.<ScmTradeDocItem>lambdaQuery()
                .eq(ScmTradeDocItem::getDocId, e.docId()).orderByAsc(ScmTradeDocItem::getId));
        record(e.kind().getOrderKind(), e.partnerId(), e.bizDate(), items);
    }

    /**
     * 登记一张单据的成交价：同一物料多行时最新价取最后一行，最低 / 最高价取各行的最低 / 最高。
     * 按物料 ID 顺序逐个原子更新（INSERT ... ON DUPLICATE KEY UPDATE），并发确认不会丢失更新，固定加锁顺序避免死锁
     */
    void record(OrderKind kind, Long partnerId, LocalDate date, List<ScmTradeDocItem> items) {
        Map<Long, List<BigDecimal>> prices = new TreeMap<>();
        for (ScmTradeDocItem i : items) {
            if (i.getPrice() == null || i.getPrice().signum() <= 0 || i.getMaterialId() == null) {
                continue;
            }
            prices.computeIfAbsent(i.getMaterialId(), k -> new ArrayList<>()).add(i.getPrice());
        }
        prices.forEach((materialId, list) -> memoryMapper.upsert(kind, partnerId, materialId, date, list.get(list.size() - 1),
                list.stream().min(BigDecimal::compareTo).orElseThrow(), list.stream().max(BigDecimal::compareTo).orElseThrow()));
    }

    /** 往来单位这些物料的价格记忆，物料 ID → 记忆 */
    public Map<Long, ScmPriceMemory> ofMaterials(OrderKind kind, Long partnerId, Collection<Long> materialIds) {
        if (partnerId == null || materialIds.isEmpty()) {
            return Map.of();
        }
        return memoryMapper.selectList(Wrappers.<ScmPriceMemory>lambdaQuery().eq(ScmPriceMemory::getKind, kind)
                        .eq(ScmPriceMemory::getPartnerId, partnerId).in(ScmPriceMemory::getMaterialId, materialIds)).stream()
                .collect(Collectors.toMap(ScmPriceMemory::getMaterialId, Function.identity(), (a, b) -> a));
    }
}
