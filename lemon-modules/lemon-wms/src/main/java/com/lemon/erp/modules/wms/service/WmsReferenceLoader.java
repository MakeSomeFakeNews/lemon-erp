package com.lemon.erp.modules.wms.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.NO_LOCATION;

/**
 * 单据校验时批量加载并校验仓库、库位、物料
 */
@Component
@RequiredArgsConstructor
class WmsReferenceLoader {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsMaterialMapper materialMapper;

    /** 启用的仓库 */
    WmsWarehouse warehouse(Long id, String label) {
        return warehouse(id, label, true);
    }

    /** @param requireEnabled 是否要求启用：出库、调出允许停用的仓库，以便清空剩余库存 */
    WmsWarehouse warehouse(Long id, String label, boolean requireEnabled) {
        WmsWarehouse warehouse = id == null ? null : warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BizException("{0}不存在", I18n.t(label));
        }
        if (requireEnabled && !Integer.valueOf(1).equals(warehouse.getStatus())) {
            throw new BizException("{0}「{1}」已停用", I18n.t(label), warehouse.getName());
        }
        return warehouse;
    }

    /**
     * 按 ID 加载物料，不存在时报错
     *
     * @param requireEnabled 是否要求启用（盘点允许停用的物料，因为可能还有库存）
     */
    Map<Long, WmsMaterial> materials(Collection<Long> ids, boolean requireEnabled) {
        Map<Long, WmsMaterial> map = materialMapper.selectByIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
        for (Long id : ids) {
            WmsMaterial material = map.get(id);
            if (material == null) {
                throw new BizException("物料不存在：{0}", id);
            }
            if (requireEnabled && !Integer.valueOf(1).equals(material.getStatus())) {
                throw new BizException("物料「{0} {1}」已停用", material.getCode(), material.getName());
            }
        }
        return map;
    }

    /** 加载库位（忽略未指定库位 0），不存在时报错 */
    Map<Long, WmsLocation> locations(Collection<Long> ids) {
        var list = ids.stream().filter(id -> id != NO_LOCATION).distinct().toList();
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<Long, WmsLocation> map = locationMapper.selectByIds(list).stream()
                .collect(Collectors.toMap(WmsLocation::getId, Function.identity()));
        if (map.size() < list.size()) {
            throw new BizException("库位不存在");
        }
        return map;
    }

    /**
     * 校验库位属于该仓库
     *
     * @param requireEnabled 是否要求启用（出库、盘点允许从停用的库位取出）
     */
    static void checkLocation(Map<Long, WmsLocation> locations, long locationId, WmsWarehouse warehouse, boolean requireEnabled) {
        if (locationId == NO_LOCATION) {
            return;
        }
        WmsLocation location = locations.get(locationId);
        if (location == null || !Objects.equals(location.getWarehouseId(), warehouse.getId())) {
            throw new BizException("库位不属于仓库「{0}」", warehouse.getName());
        }
        if (requireEnabled && !Integer.valueOf(1).equals(location.getStatus())) {
            throw new BizException("库位「{0}」已停用", location.getCode());
        }
    }
}
