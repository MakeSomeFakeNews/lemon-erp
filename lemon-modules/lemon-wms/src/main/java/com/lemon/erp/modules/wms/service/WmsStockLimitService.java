package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.modules.wms.dto.WarehouseAlertView;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialWarehouseLimit;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialWarehouseLimitMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.StockAlertMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存上下限（issue #111）：物料的预警方式为按总库存时，上下限是物料上的安全库存、库存上限；
 * 分仓库时每个仓库单独设置（{@code wms_material_warehouse_limit}），物料上的安全库存、库存上限保存为各仓库设置之和。
 * 预警的判断口径在 WmsMaterialWarehouseLimitMapper.xml 的 materialAlerts 片段中
 */
@Service
@RequiredArgsConstructor
public class WmsStockLimitService {

    private final WmsMaterialWarehouseLimitMapper limitMapper;
    private final WmsWarehouseMapper warehouseMapper;
    private final WmsMaterialMapper materialMapper;

    /** 物料分仓库的上下限，按仓库排序 */
    public List<WmsMaterialWarehouseLimit> list(Long materialId) {
        return limitMapper.selectList(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery()
                .eq(WmsMaterialWarehouseLimit::getMaterialId, materialId)
                .orderByAsc(WmsMaterialWarehouseLimit::getId));
    }

    /** 物料各仓库的当前库存与上下限（分仓库预警的明细） */
    public List<WarehouseAlertView> warehouseAlerts(Long materialId) {
        return limitMapper.selectWarehouseAlerts(materialId);
    }

    /**
     * 保存物料前规范化预警设置：方式为空时按总库存；按总库存时检查安全库存不大于库存上限；
     * 分仓库时校验仓库明细（没有传入时取已保存的），并把物料的安全库存、库存上限设为各仓库之和
     *
     * @param materialId 修改时为物料 ID，新增时为 null
     * @return 分仓库时要保存的明细；按总库存时为空列表
     */
    public List<WmsMaterialWarehouseLimit> prepare(WmsMaterial material, Long materialId) {
        if (material.getAlertMode() != StockAlertMode.WAREHOUSE) {
            material.setAlertMode(StockAlertMode.TOTAL);
            checkRange(material.getSafetyStock(), material.getMaxStock());
            return List.of();
        }
        List<WmsMaterialWarehouseLimit> rows = material.getWarehouseLimits() != null ? normalize(material.getWarehouseLimits())
                : materialId == null ? List.of() : list(materialId);
        material.setSafetyStock(sum(rows, WmsMaterialWarehouseLimit::getMinQty));
        material.setMaxStock(sum(rows, WmsMaterialWarehouseLimit::getMaxQty));
        return rows;
    }

    /**
     * 物料保存后写入分仓库的明细（在调用方的事务中）：按总库存时删除原有设置；分仓库时传入了明细则整体替换，没有传入时保持不变
     */
    public void save(WmsMaterial material, List<WmsMaterialWarehouseLimit> rows) {
        if (material.getAlertMode() == StockAlertMode.WAREHOUSE && material.getWarehouseLimits() == null) {
            return;
        }
        List<WmsMaterialWarehouseLimit> items = material.getAlertMode() == StockAlertMode.WAREHOUSE ? rows : List.of();
        items.forEach(r -> r.setId(null));
        DetailLines.replace(limitMapper, WmsMaterialWarehouseLimit::getMaterialId, material.getId(), items,
                WmsMaterialWarehouseLimit::setMaterialId, limitMapper::insertBatch);
    }

    /**
     * 批量设置某个仓库的上下限（分仓库），两者都为空时删除该仓库的设置；之后重算这些物料的合计。物料的预警方式由调用方改为分仓库
     */
    public void setWarehouse(Collection<Long> materialIds, Long warehouseId, BigDecimal minQty, BigDecimal maxQty) {
        WmsWarehouse warehouse = warehouseId == null ? null : warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new BizException("wms.error.stockLimitWarehouseRequired");
        }
        checkRange(minQty, maxQty);
        limitMapper.delete(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery()
                .in(WmsMaterialWarehouseLimit::getMaterialId, materialIds)
                .eq(WmsMaterialWarehouseLimit::getWarehouseId, warehouseId));
        if (minQty != null || maxQty != null) {
            List<WmsMaterialWarehouseLimit> rows = new ArrayList<>(materialIds.size());
            for (Long id : materialIds) {
                WmsMaterialWarehouseLimit r = new WmsMaterialWarehouseLimit();
                r.setMaterialId(id);
                r.setWarehouseId(warehouseId);
                r.setMinQty(minQty);
                r.setMaxQty(maxQty);
                rows.add(r);
            }
            limitMapper.insertBatch(rows);
        }
    }

    /** 删除物料的分仓库设置（物料删除、批量改为按总库存时） */
    public void deleteByMaterials(Collection<Long> materialIds) {
        if (!materialIds.isEmpty()) {
            limitMapper.delete(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery().in(WmsMaterialWarehouseLimit::getMaterialId, materialIds));
        }
    }

    /** 删除仓库时删除该仓库的上下限，并重算涉及物料的合计 */
    public void deleteByWarehouses(Collection<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return;
        }
        Set<Long> materialIds = limitMapper.selectList(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery()
                        .select(WmsMaterialWarehouseLimit::getMaterialId)
                        .in(WmsMaterialWarehouseLimit::getWarehouseId, warehouseIds)).stream()
                .map(WmsMaterialWarehouseLimit::getMaterialId).collect(Collectors.toSet());
        if (materialIds.isEmpty()) {
            return;
        }
        limitMapper.delete(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery().in(WmsMaterialWarehouseLimit::getWarehouseId, warehouseIds));
        refreshTotals(materialIds);
    }

    /** 重算分仓库预警的物料上的安全库存、库存上限（各仓库设置之和，没有设置时为空） */
    public void refreshTotals(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return;
        }
        Map<Long, List<WmsMaterialWarehouseLimit>> byMaterial = limitMapper.selectList(Wrappers.<WmsMaterialWarehouseLimit>lambdaQuery()
                        .in(WmsMaterialWarehouseLimit::getMaterialId, materialIds)).stream()
                .collect(Collectors.groupingBy(WmsMaterialWarehouseLimit::getMaterialId));
        List<WmsMaterial> materials = materialMapper.selectList(Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId).in(WmsMaterial::getId, materialIds).eq(WmsMaterial::getAlertMode, StockAlertMode.WAREHOUSE));
        for (WmsMaterial m : materials) {
            List<WmsMaterialWarehouseLimit> rows = byMaterial.getOrDefault(m.getId(), List.of());
            materialMapper.update(Wrappers.<WmsMaterial>lambdaUpdate()
                    .set(WmsMaterial::getSafetyStock, sum(rows, WmsMaterialWarehouseLimit::getMinQty))
                    .set(WmsMaterial::getMaxStock, sum(rows, WmsMaterialWarehouseLimit::getMaxQty))
                    .set(WmsMaterial::getUpdateTime, LocalDateTime.now())
                    .eq(WmsMaterial::getId, m.getId()));
        }
    }

    /** 安全库存（下限）不能大于库存上限 */
    public static void checkRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BizException("wms.error.stockLimitMinGreaterMax", min.stripTrailingZeros().toPlainString(),
                    max.stripTrailingZeros().toPlainString());
        }
    }

    /** 分仓库明细：必须选择仓库且不重复、仓库存在，下限不大于上限；上下限都没填的行忽略 */
    private List<WmsMaterialWarehouseLimit> normalize(List<WmsMaterialWarehouseLimit> input) {
        Set<Long> ids = new HashSet<>();
        for (WmsMaterialWarehouseLimit r : input) {
            if (r == null || r.getWarehouseId() == null) {
                throw new BizException("wms.error.stockLimitWarehouseRequired");
            }
            ids.add(r.getWarehouseId());
        }
        Map<Long, WmsWarehouse> warehouses = ids.isEmpty() ? Map.of() : warehouseMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(WmsWarehouse::getId, Function.identity()));
        Map<Long, WmsMaterialWarehouseLimit> rows = new LinkedHashMap<>();
        for (WmsMaterialWarehouseLimit r : input) {
            WmsWarehouse w = warehouses.get(r.getWarehouseId());
            if (w == null) {
                throw new BizException("wms.error.warehouseDoesNotExist");
            }
            if (rows.containsKey(w.getId())) {
                throw new BizException("wms.error.stockLimitWarehouseDuplicate", w.getName());
            }
            if (r.getMinQty() != null && r.getMaxQty() != null && r.getMinQty().compareTo(r.getMaxQty()) > 0) {
                throw new BizException("wms.error.warehouseStockLimitMinGreaterMax", w.getName());
            }
            if (r.getMinQty() == null && r.getMaxQty() == null) {
                continue;
            }
            WmsMaterialWarehouseLimit row = new WmsMaterialWarehouseLimit();
            row.setWarehouseId(w.getId());
            row.setMinQty(r.getMinQty());
            row.setMaxQty(r.getMaxQty());
            rows.put(w.getId(), row);
        }
        return new ArrayList<>(rows.values());
    }

    /** 各行某个值之和，都为空时为空 */
    private static BigDecimal sum(List<WmsMaterialWarehouseLimit> rows, Function<WmsMaterialWarehouseLimit, BigDecimal> getter) {
        List<BigDecimal> values = rows.stream().map(getter).filter(Objects::nonNull).toList();
        return values.isEmpty() ? null : Decimals.sum(values);
    }
}
