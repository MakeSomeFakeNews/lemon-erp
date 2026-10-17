package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsReservation;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存预留。预留不锁定具体库位、批次，只保证仓库中「可用库存 ≥ 全部预留」：
 * 预留时校验空闲库存（可用 - 已预留），出库、调拨等扣减库存后校验扣减的物料仍满足预留（盘点盘亏除外）。
 * 来源出库时先核销自己的预留，再扣减库存
 */
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final WmsReservationMapper reservationMapper;
    private final WmsMaterialMapper materialMapper;

    public PageResult<WmsReservation> page(long current, long size, Long warehouseId, String keyword, String sourceNo) {
        Page<WmsReservation> page = Page.of(current, size);
        reservationMapper.selectPageList(page, warehouseId, keyword, sourceNo);
        return PageResult.of(page);
    }

    public List<WmsReservation> bySource(String sourceType, Long sourceId) {
        return reservationMapper.selectBySource(sourceType, sourceId);
    }

    /** 仓库中各物料的空闲库存（可用 - 已预留），可能为负（盘亏后） */
    public Map<Long, BigDecimal> free(Long warehouseId, Collection<Long> materialIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        for (Map<String, Object> row : reservationMapper.selectFree(warehouseId, materialIds)) {
            map.put(((Number) row.get("materialId")).longValue(), dec(row.get("available")).subtract(dec(row.get("reserved"))));
        }
        return map;
    }

    /** 已预留数量，warehouseId 为空表示全部仓库 */
    public BigDecimal reserved(Long warehouseId, Long materialId) {
        return reservationMapper.selectList(Wrappers.<WmsReservation>lambdaQuery()
                        .eq(warehouseId != null, WmsReservation::getWarehouseId, warehouseId)
                        .eq(WmsReservation::getMaterialId, materialId).gt(WmsReservation::getQty, 0))
                .stream().map(WmsReservation::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 预留：数量不超过空闲库存 */
    @Transactional
    public void reserve(String sourceType, Long sourceId, Long sourceItemId, String sourceNo, Long warehouseId, Long materialId,
                        BigDecimal qty, String remark) {
        if (qty.signum() <= 0) {
            throw new BizException("wms.error.reservedQuantityMustGreater");
        }
        // 锁定物料行，避免并发预留同一物料超出空闲库存
        WmsMaterial material = materialMapper.selectOne(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getId, materialId).last("FOR UPDATE"));
        BigDecimal free = free(warehouseId, List.of(materialId)).getOrDefault(materialId, BigDecimal.ZERO);
        if (free.compareTo(qty) < 0) {
            throw new BizException("wms.error.insufficientFreeStockReservableRequired", material.getCode(), material.getName(),
                    plain(free.max(BigDecimal.ZERO)), plain(qty));
        }
        WmsReservation r = new WmsReservation();
        r.setWarehouseId(warehouseId);
        r.setMaterialId(materialId);
        r.setQty(qty);
        r.setSourceType(sourceType);
        r.setSourceId(sourceId);
        r.setSourceItemId(sourceItemId);
        r.setSourceNo(sourceNo);
        r.setRemark(remark);
        r.setCreateBy(SecurityUtils.getUserIdOrNull());
        reservationMapper.insert(r);
    }

    /** 来源出库：核销该明细在该仓库的预留 */
    @Transactional
    public void consume(String sourceType, Long sourceItemId, Long warehouseId, BigDecimal qty) {
        BigDecimal left = qty;
        for (WmsReservation r : reservationMapper.selectList(Wrappers.<WmsReservation>lambdaQuery()
                .eq(WmsReservation::getSourceType, sourceType).eq(WmsReservation::getSourceItemId, sourceItemId)
                .eq(WmsReservation::getWarehouseId, warehouseId).gt(WmsReservation::getQty, 0).orderByAsc(WmsReservation::getId))) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal take = left.min(r.getQty());
            reservationMapper.consume(r.getId(), take);
            left = left.subtract(take);
        }
    }

    /** 释放来源的全部预留 */
    @Transactional
    public void release(String sourceType, Long sourceId) {
        reservationMapper.update(Wrappers.<WmsReservation>lambdaUpdate().set(WmsReservation::getQty, BigDecimal.ZERO)
                .eq(WmsReservation::getSourceType, sourceType).eq(WmsReservation::getSourceId, sourceId).gt(WmsReservation::getQty, 0));
    }

    /** 释放来源明细的全部预留（订单变更减少数量、删除明细时） */
    @Transactional
    public void releaseItem(String sourceType, Long sourceItemId) {
        reservationMapper.update(Wrappers.<WmsReservation>lambdaUpdate().set(WmsReservation::getQty, BigDecimal.ZERO)
                .eq(WmsReservation::getSourceType, sourceType).eq(WmsReservation::getSourceItemId, sourceItemId).gt(WmsReservation::getQty, 0));
    }

    @Transactional
    public void releaseById(Long id) {
        if (reservationMapper.update(Wrappers.<WmsReservation>lambdaUpdate().set(WmsReservation::getQty, BigDecimal.ZERO)
                .eq(WmsReservation::getId, id).gt(WmsReservation::getQty, 0)) == 0) {
            throw new BizException("wms.error.reservationDoesNotExistHasReleased");
        }
    }

    public WmsReservation get(Long id) {
        WmsReservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException("wms.error.reservationDoesNotExist");
        }
        return r;
    }

    /**
     * 扣减库存后校验：扣减的物料在仓库中的可用库存仍不少于预留
     *
     * @param pairs 仓库 ID → 物料 ID 集合
     */
    void checkAfterDecrease(Map<Long, Set<Long>> pairs) {
        pairs.forEach((warehouseId, materialIds) -> free(warehouseId, materialIds).forEach((materialId, free) -> {
            if (free.signum() < 0) {
                WmsMaterial m = materialMapper.selectById(materialId);
                BigDecimal reserved = reservationMapper.selectList(Wrappers.<WmsReservation>lambdaQuery()
                                .eq(WmsReservation::getWarehouseId, warehouseId).eq(WmsReservation::getMaterialId, materialId)
                                .gt(WmsReservation::getQty, 0)).stream().map(WmsReservation::getQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String sources = reservationMapper.selectList(Wrappers.<WmsReservation>lambdaQuery()
                                .eq(WmsReservation::getWarehouseId, warehouseId).eq(WmsReservation::getMaterialId, materialId)
                                .gt(WmsReservation::getQty, 0).last("LIMIT 3")).stream().map(WmsReservation::getSourceNo)
                        .collect(Collectors.joining(Messages.get("wms.text.listSeparator")));
                throw new BizException("wms.error.stockReservedTotalEtcAfterOutbound",
                        m.getCode(), m.getName(), plain(reserved), sources);
            }
        }));
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
