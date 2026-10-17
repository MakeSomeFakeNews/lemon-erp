package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.model.LocationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WmsLocationService {

    private final WmsLocationMapper locationMapper;
    private final WmsStockMapper stockMapper;
    private final WmsWarehouseService warehouseService;

    public PageResult<WmsLocation> page(long current, long size, Long warehouseId, String keyword, Integer status) {
        Page<WmsLocation> page = Page.of(current, size);
        locationMapper.selectPageList(page, warehouseId, keyword, status);
        return PageResult.of(page);
    }

    public WmsLocation get(Long id) {
        WmsLocation location = id == null ? null : locationMapper.selectById(id);
        if (location == null) {
            throw new BizException("wms.error.locationDoesNotExist");
        }
        return location;
    }

    /** 仓库下启用的库位，供单据等页面下拉选择 */
    public List<OptionItem> options(Long warehouseId) {
        return locationMapper.selectList(Wrappers.<WmsLocation>lambdaQuery()
                        .select(WmsLocation::getId, WmsLocation::getCode, WmsLocation::getName)
                        .eq(WmsLocation::getWarehouseId, warehouseId)
                        .eq(WmsLocation::getStatus, 1)
                        .orderByAsc(WmsLocation::getSort)
                        .orderByAsc(WmsLocation::getCode))
                .stream().map(l -> new OptionItem(l.getId(), l.getCode(), l.getName())).toList();
    }

    public void create(WmsLocation location) {
        warehouseService.get(location.getWarehouseId());
        checkCodeUnique(location.getWarehouseId(), location.getCode(), null);
        location.setId(null);
        if (location.getStatus() == null) location.setStatus(1);
        if (location.getSort() == null) location.setSort(0);
        if (location.getLocType() == null) location.setLocType(LocationType.STORAGE);
        locationMapper.insert(location);
    }

    /** 所属仓库不能修改：库存、单据都按「仓库 + 库位」记录 */
    public void update(Long id, WmsLocation location) {
        WmsLocation old = get(id);
        if (!Objects.equals(old.getWarehouseId(), location.getWarehouseId())) {
            throw new BizException("wms.error.warehouseLocationCannotChanged");
        }
        checkCodeUnique(old.getWarehouseId(), location.getCode(), id);
        location.setId(id);
        if (location.getSort() == null) location.setSort(0);
        if (location.getLocType() == null) location.setLocType(LocationType.STORAGE);
        location.setStatus(null);
        locationMapper.updateById(location);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        WmsLocation entity = new WmsLocation();
        entity.setId(id);
        entity.setStatus(status);
        locationMapper.updateById(entity);
    }

    /** 产生过库存的库位不能删除，可以停用 */
    @Transactional
    public void delete(List<Long> ids) {
        for (WmsLocation location : locationMapper.selectByIds(ids)) {
            if (stockMapper.exists(Wrappers.<WmsStock>lambdaQuery()
                    .eq(WmsStock::getWarehouseId, location.getWarehouseId())
                    .eq(WmsStock::getLocationId, location.getId()))) {
                throw new BizException("wms.error.locationHasStockRecordsCannotDeleted", location.getCode());
            }
        }
        locationMapper.deleteByIds(ids);
    }

    private void checkCodeUnique(Long warehouseId, String code, Long id) {
        WmsLocation exist = locationMapper.selectOne(Wrappers.<WmsLocation>lambdaQuery()
                .eq(WmsLocation::getWarehouseId, warehouseId)
                .eq(WmsLocation::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("wms.error.locationCodeAlreadyExistsWarehouse");
        }
    }
}
