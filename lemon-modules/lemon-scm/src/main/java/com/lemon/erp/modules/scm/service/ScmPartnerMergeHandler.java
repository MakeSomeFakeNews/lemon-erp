package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import com.lemon.erp.modules.scm.entity.ScmPriceMemory;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.entity.ScmSupplierPriceTier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerPriceMapper;
import com.lemon.erp.modules.scm.mapper.ScmMergeMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceMemoryMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceTierMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 往来单位合并：本模块的订单、出入库单、草稿、报价、发货通知、发运、返利、渠道库存、价目表、请购、询价、评估、采购合同、招标、ASN 改为保留的资料。
 * 有唯一约束的价格按以下规则合并（同一物料只保留一行）：
 * <ul>
 *     <li>客户执行价、供货价格：保留资料已有该物料的价格时以保留资料为准，被合并资料的删除；没有时取被合并资料中最近修改的一条。
 *     被删除的供货价格是该物料的默认供应商时，保留的那条设为默认（同一单位）；阶梯价随价格一起删除</li>
 *     <li>价格记忆：同一物料合为一行，最新价取日期最近的，最低价、最高价取各行的最低 / 最高</li>
 * </ul>
 * 不能自动处理的冲突拒绝合并：两份资料参与了同一张询价单、同一次招标（邀请或投标），两个经销商上报了同一期间的渠道库存
 */
@Component
@RequiredArgsConstructor
public class ScmPartnerMergeHandler implements PartnerMergeHandler {

    private static final List<String> CUSTOMER_REFS = List.of("salesOrder", "salesTradeDoc", "salesDraft", "quotation", "delivery",
            "shipment", "salesRebate", "channelReport", "priceList");
    private static final List<String> SUPPLIER_REFS = List.of("purchaseOrder", "purchaseTradeDoc", "purchaseDraft", "carrierTradeDoc",
            "carrierDraft", "carrierShipment", "requisition", "inquiryQuote", "inquirySupplier", "supplierEval", "purchaseRebate",
            "purchaseContract", "tenderAward", "tenderSupplier", "bid", "asn");
    private static final int YES = 1;

    private final ScmMergeMapper mergeMapper;
    private final ScmCustomerPriceMapper customerPriceMapper;
    private final ScmSupplierPriceMapper supplierPriceMapper;
    private final ScmSupplierPriceTierMapper tierMapper;
    private final ScmPriceMemoryMapper memoryMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        boolean customer = type == PartnerType.CUSTOMER;
        List<MergeImpact> list = new ArrayList<>(customer ? mergeMapper.customerImpacts(sourceIds) : mergeMapper.supplierImpacts(sourceIds));
        if (customer) {
            List<Long> source = customerPriceMapper.selectList(customerPrices(sourceIds)).stream().map(ScmCustomerPrice::getMaterialId).toList();
            Set<Long> target = customerPriceMapper.selectList(customerPrices(List.of(targetId))).stream()
                    .map(ScmCustomerPrice::getMaterialId).collect(Collectors.toSet());
            list.add(new MergeImpact("scm.merge.customerPrice", source.size(), duplicates(source, target)));
        } else {
            List<Long> source = supplierPriceMapper.selectList(supplierPrices(sourceIds)).stream().map(ScmSupplierPrice::getMaterialId).toList();
            Set<Long> target = supplierPriceMapper.selectList(supplierPrices(List.of(targetId))).stream()
                    .map(ScmSupplierPrice::getMaterialId).collect(Collectors.toSet());
            list.add(new MergeImpact("scm.merge.supplierPrice", source.size(), duplicates(source, target)));
        }
        OrderKind kind = customer ? OrderKind.SALES : OrderKind.PURCHASE;
        List<Long> source = memoryMapper.selectList(memories(kind, sourceIds)).stream().map(ScmPriceMemory::getMaterialId).toList();
        Set<Long> target = memoryMapper.selectList(memories(kind, List.of(targetId))).stream()
                .map(ScmPriceMemory::getMaterialId).collect(Collectors.toSet());
        list.add(new MergeImpact("scm.merge.priceMemory", source.size(), duplicates(source, target)));
        return list;
    }

    @Override
    public void check(PartnerType type, List<Long> sourceIds, Long targetId) {
        List<Long> all = Stream.concat(sourceIds.stream(), Stream.of(targetId)).toList();
        if (type == PartnerType.CUSTOMER) {
            List<Integer> periods = mergeMapper.sharedChannelPeriods(all);
            if (!periods.isEmpty()) {
                throw new BizException("scm.error.mergeSharedChannelReport", periods.get(0));
            }
            return;
        }
        List<String> inquiries = mergeMapper.sharedInquiries(all);
        if (!inquiries.isEmpty()) {
            throw new BizException("scm.error.mergeSharedInquiry", inquiries.get(0));
        }
        List<String> tenders = mergeMapper.sharedTenders(all);
        if (!tenders.isEmpty()) {
            throw new BizException("scm.error.mergeSharedTender", tenders.get(0));
        }
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        boolean customer = type == PartnerType.CUSTOMER;
        (customer ? CUSTOMER_REFS : SUPPLIER_REFS).forEach(ref -> mergeMapper.mergeRef(ref, sourceIds, targetId));
        if (customer) {
            mergeCustomerPrices(sourceIds, targetId);
        } else {
            mergeSupplierPrices(sourceIds, targetId);
        }
        mergeMemories(customer ? OrderKind.SALES : OrderKind.PURCHASE, sourceIds, targetId);
    }

    /** 被合并资料中与保留资料重复（或彼此重复）的物料行数：这些行合并或删除，其余直接改指向 */
    private static long duplicates(List<Long> sourceMaterials, Set<Long> targetMaterials) {
        Set<Long> seen = new java.util.HashSet<>(targetMaterials);
        return sourceMaterials.stream().filter(m -> !seen.add(m)).count();
    }

    private void mergeCustomerPrices(List<Long> sourceIds, Long targetId) {
        Set<Long> taken = customerPriceMapper.selectList(customerPrices(List.of(targetId))).stream()
                .map(ScmCustomerPrice::getMaterialId).collect(Collectors.toSet());
        List<Long> drop = new ArrayList<>();
        for (ScmCustomerPrice p : latestFirst(customerPriceMapper.selectList(customerPrices(sourceIds)), ScmCustomerPrice::getUpdateTime,
                ScmCustomerPrice::getId)) {
            if (!taken.add(p.getMaterialId())) {
                drop.add(p.getId());
            }
        }
        if (!drop.isEmpty()) {
            customerPriceMapper.deleteByIds(drop);
        }
        customerPriceMapper.update(Wrappers.<ScmCustomerPrice>lambdaUpdate().set(ScmCustomerPrice::getCustomerId, targetId)
                .in(ScmCustomerPrice::getCustomerId, sourceIds));
    }

    private void mergeSupplierPrices(List<Long> sourceIds, Long targetId) {
        // 物料 → 保留的价格行（保留资料的优先，其次被合并资料中最近修改的）
        Map<Long, ScmSupplierPrice> kept = new HashMap<>();
        supplierPriceMapper.selectList(supplierPrices(List.of(targetId))).forEach(p -> kept.put(p.getMaterialId(), p));
        List<Long> drop = new ArrayList<>();
        List<Long> makeDefault = new ArrayList<>();
        for (ScmSupplierPrice p : latestFirst(supplierPriceMapper.selectList(supplierPrices(sourceIds)), ScmSupplierPrice::getUpdateTime,
                ScmSupplierPrice::getId)) {
            ScmSupplierPrice keep = kept.putIfAbsent(p.getMaterialId(), p);
            if (keep == null) {
                continue;
            }
            drop.add(p.getId());
            if (Objects.equals(p.getIsDefault(), YES) && !Objects.equals(keep.getIsDefault(), YES)) {
                keep.setIsDefault(YES);
                makeDefault.add(keep.getId());
            }
        }
        if (!drop.isEmpty()) {
            tierMapper.delete(Wrappers.<ScmSupplierPriceTier>lambdaQuery().in(ScmSupplierPriceTier::getPriceId, drop));
            supplierPriceMapper.deleteByIds(drop);
        }
        if (!makeDefault.isEmpty()) {
            supplierPriceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate().set(ScmSupplierPrice::getIsDefault, YES)
                    .in(ScmSupplierPrice::getId, makeDefault));
        }
        supplierPriceMapper.update(Wrappers.<ScmSupplierPrice>lambdaUpdate().set(ScmSupplierPrice::getSupplierId, targetId)
                .in(ScmSupplierPrice::getSupplierId, sourceIds));
    }

    private void mergeMemories(OrderKind kind, List<Long> sourceIds, Long targetId) {
        Map<Long, ScmPriceMemory> kept = new LinkedHashMap<>();
        memoryMapper.selectList(memories(kind, List.of(targetId))).forEach(m -> kept.put(m.getMaterialId(), m));
        List<Long> drop = new ArrayList<>();
        Map<Long, ScmPriceMemory> changed = new LinkedHashMap<>();
        for (ScmPriceMemory m : memoryMapper.selectList(memories(kind, sourceIds).orderByAsc(ScmPriceMemory::getId))) {
            ScmPriceMemory keep = kept.putIfAbsent(m.getMaterialId(), m);
            if (keep == null) {
                continue;
            }
            drop.add(m.getId());
            if (m.getLastDate().isAfter(keep.getLastDate())) {
                keep.setLastDate(m.getLastDate());
                keep.setLastPrice(m.getLastPrice());
            }
            if (m.getMinPrice().compareTo(keep.getMinPrice()) < 0) {
                keep.setMinDate(m.getMinDate());
                keep.setMinPrice(m.getMinPrice());
            }
            if (m.getMaxPrice().compareTo(keep.getMaxPrice()) > 0) {
                keep.setMaxDate(m.getMaxDate());
                keep.setMaxPrice(m.getMaxPrice());
            }
            changed.put(keep.getId(), keep);
        }
        if (!drop.isEmpty()) {
            memoryMapper.deleteByIds(drop);
        }
        changed.values().forEach(m -> memoryMapper.update(Wrappers.<ScmPriceMemory>lambdaUpdate()
                .set(ScmPriceMemory::getLastDate, m.getLastDate()).set(ScmPriceMemory::getLastPrice, m.getLastPrice())
                .set(ScmPriceMemory::getMinDate, m.getMinDate()).set(ScmPriceMemory::getMinPrice, m.getMinPrice())
                .set(ScmPriceMemory::getMaxDate, m.getMaxDate()).set(ScmPriceMemory::getMaxPrice, m.getMaxPrice())
                .set(ScmPriceMemory::getUpdateTime, LocalDateTime.now())
                .eq(ScmPriceMemory::getId, m.getId())));
        memoryMapper.update(Wrappers.<ScmPriceMemory>lambdaUpdate().set(ScmPriceMemory::getPartnerId, targetId)
                .eq(ScmPriceMemory::getKind, kind).in(ScmPriceMemory::getPartnerId, sourceIds));
    }

    /** 按修改时间倒序（最近修改的优先保留），时间相同按 ID 倒序 */
    private static <T> List<T> latestFirst(List<T> rows, java.util.function.Function<T, LocalDateTime> time, java.util.function.Function<T, Long> id) {
        return rows.stream().sorted(Comparator.comparing(time, Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparing(id).reversed()).toList();
    }

    private static com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScmCustomerPrice> customerPrices(List<Long> ids) {
        return Wrappers.<ScmCustomerPrice>lambdaQuery()
                .select(ScmCustomerPrice::getId, ScmCustomerPrice::getMaterialId, ScmCustomerPrice::getUpdateTime)
                .in(ScmCustomerPrice::getCustomerId, ids);
    }

    private static com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScmSupplierPrice> supplierPrices(List<Long> ids) {
        return Wrappers.<ScmSupplierPrice>lambdaQuery()
                .select(ScmSupplierPrice::getId, ScmSupplierPrice::getMaterialId, ScmSupplierPrice::getIsDefault, ScmSupplierPrice::getUpdateTime)
                .in(ScmSupplierPrice::getSupplierId, ids);
    }

    private static com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScmPriceMemory> memories(OrderKind kind, List<Long> ids) {
        return Wrappers.<ScmPriceMemory>lambdaQuery().eq(ScmPriceMemory::getKind, kind).in(ScmPriceMemory::getPartnerId, ids);
    }
}
