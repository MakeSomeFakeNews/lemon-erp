package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.PackageRequest;
import com.lemon.erp.modules.scm.entity.ScmDelivery;
import com.lemon.erp.modules.scm.entity.ScmDeliveryItem;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPackage;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPackageItem;
import com.lemon.erp.modules.scm.mapper.ScmDeliveryPackageMapper;
import com.lemon.erp.modules.scm.model.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 装箱打包：已拣货（或已出库）的发货通知按箱录入，箱号 = 发货通知号-两位序号；每个通知明细的装箱合计不超过通知数量
 */
@Service
@RequiredArgsConstructor
public class ScmPackingService {

    private final ScmDeliveryPackageMapper packageMapper;
    private final ScmDeliveryService deliveryService;

    /** 箱列表（含箱内物料） */
    public List<ScmDeliveryPackage> packages(Long deliveryId) {
        deliveryService.detail(deliveryId);
        List<ScmDeliveryPackage> packages = packageMapper.selectList(Wrappers.<ScmDeliveryPackage>lambdaQuery()
                .eq(ScmDeliveryPackage::getDeliveryId, deliveryId).orderByAsc(ScmDeliveryPackage::getSeq));
        Map<Long, List<ScmDeliveryPackageItem>> items = packageMapper.selectItems(deliveryId).stream()
                .collect(Collectors.groupingBy(ScmDeliveryPackageItem::getPackageId));
        packages.forEach(p -> p.setItems(items.getOrDefault(p.getId(), List.of())));
        return packages;
    }

    @Transactional
    public Long create(Long deliveryId, PackageRequest req) {
        ScmDelivery d = requirePackable(deliveryId);
        List<ScmDeliveryPackageItem> items = toItems(d, req, null);
        Integer max = packageMapper.selectList(Wrappers.<ScmDeliveryPackage>lambdaQuery().select(ScmDeliveryPackage::getSeq)
                .eq(ScmDeliveryPackage::getDeliveryId, deliveryId)).stream().map(ScmDeliveryPackage::getSeq).max(Integer::compareTo).orElse(0);
        ScmDeliveryPackage p = new ScmDeliveryPackage();
        p.setDeliveryId(deliveryId);
        p.setSeq(max + 1);
        p.setBoxNo(String.format("%s-%02d", d.getDeliveryNo(), max + 1));
        p.setWeight(req.weight());
        p.setVolume(req.volume());
        p.setRemark(req.remark());
        packageMapper.insert(p);
        packageMapper.insertItems(p.getId(), items);
        return p.getId();
    }

    @Transactional
    public void update(Long deliveryId, Long packageId, PackageRequest req) {
        ScmDelivery d = requirePackable(deliveryId);
        ScmDeliveryPackage p = requirePackage(deliveryId, packageId);
        List<ScmDeliveryPackageItem> items = toItems(d, req, packageId);
        packageMapper.update(Wrappers.<ScmDeliveryPackage>lambdaUpdate().set(ScmDeliveryPackage::getWeight, req.weight())
                .set(ScmDeliveryPackage::getVolume, req.volume()).set(ScmDeliveryPackage::getRemark, req.remark())
                .eq(ScmDeliveryPackage::getId, p.getId()));
        packageMapper.deleteItems(packageId);
        packageMapper.insertItems(packageId, items);
    }

    @Transactional
    public void delete(Long deliveryId, Long packageId) {
        requirePackable(deliveryId);
        requirePackage(deliveryId, packageId);
        packageMapper.deleteItems(packageId);
        packageMapper.deleteById(packageId);
    }

    private List<ScmDeliveryPackageItem> toItems(ScmDelivery d, PackageRequest req, Long excludePackageId) {
        Map<Long, ScmDeliveryItem> deliveryItems = d.getItems().stream().collect(Collectors.toMap(ScmDeliveryItem::getId, Function.identity()));
        Map<Long, BigDecimal> packed = new HashMap<>();
        packageMapper.selectPackedQty(d.getId(), excludePackageId).forEach(r ->
                packed.put(((Number) r.get("deliveryItemId")).longValue(), new BigDecimal(r.get("qty").toString())));
        Map<Long, BigDecimal> thisBox = new HashMap<>();
        return req.items().stream().map(l -> {
            ScmDeliveryItem item = deliveryItems.get(l.deliveryItemId());
            if (item == null) {
                throw new BizException("scm.error.boxMaterialDoesNotBelongDelivery");
            }
            BigDecimal total = packed.getOrDefault(item.getId(), BigDecimal.ZERO).add(thisBox.merge(item.getId(), l.qty(), BigDecimal::add));
            if (total.compareTo(item.getQty()) > 0) {
                throw new BizException("scm.error.totalPackedQuantityExceedsNoticeQuantity", item.getMaterialCode(), item.getMaterialName(),
                        plain(total), plain(item.getQty()));
            }
            ScmDeliveryPackageItem i = new ScmDeliveryPackageItem();
            i.setDeliveryItemId(item.getId());
            i.setMaterialId(item.getMaterialId());
            i.setQty(l.qty());
            return i;
        }).toList();
    }

    private ScmDelivery requirePackable(Long deliveryId) {
        ScmDelivery d = deliveryService.detail(deliveryId);
        if (d.getStatus() != DeliveryStatus.PICKED && d.getStatus() != DeliveryStatus.SHIPPED) {
            throw new BizException("scm.error.deliveryNoticeMustPickedBeforePacking");
        }
        return d;
    }

    private ScmDeliveryPackage requirePackage(Long deliveryId, Long packageId) {
        ScmDeliveryPackage p = packageMapper.selectById(packageId);
        if (p == null || !p.getDeliveryId().equals(deliveryId)) {
            throw new BizException("scm.error.boxDoesNotExist");
        }
        return p;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
