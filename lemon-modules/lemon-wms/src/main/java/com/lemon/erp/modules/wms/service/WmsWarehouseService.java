package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsCheckMapper;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.WarehouseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WmsWarehouseService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsStockMapper stockMapper;
    private final WmsStockDocMapper docMapper;
    private final WmsCheckMapper checkMapper;
    private final ReferenceGuard referenceGuard;

    /** @param keyword 编码或名称，模糊匹配 */
    public PageResult<WmsWarehouse> page(long current, long size, String keyword, Integer status) {
        Page<WmsWarehouse> page = warehouseMapper.selectPage(Page.of(current, size), Wrappers.<WmsWarehouse>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(WmsWarehouse::getCode, keyword).or().like(WmsWarehouse::getName, keyword))
                .eq(status != null, WmsWarehouse::getStatus, status)
                .orderByAsc(WmsWarehouse::getSort)
                .orderByAsc(WmsWarehouse::getId));
        return PageResult.of(page);
    }

    public WmsWarehouse get(Long id) {
        WmsWarehouse warehouse = id == null ? null : warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BizException("wms.error.warehouseDoesNotExist");
        }
        return warehouse;
    }

    /** 启用的仓库，供单据等页面下拉选择 */
    public List<OptionItem> options() {
        return warehouseMapper.selectList(Wrappers.<WmsWarehouse>lambdaQuery()
                        .select(WmsWarehouse::getId, WmsWarehouse::getCode, WmsWarehouse::getName, WmsWarehouse::getType)
                        .eq(WmsWarehouse::getStatus, 1)
                        .orderByAsc(WmsWarehouse::getSort)
                        .orderByAsc(WmsWarehouse::getId))
                .stream().map(w -> new OptionItem(w.getId(), w.getCode(), w.getName(), w.getType() == null ? null : w.getType().name())).toList();
    }

    public void create(WmsWarehouse warehouse) {
        checkCodeUnique(warehouse.getCode(), null);
        warehouse.setId(null);
        warehouse.setType(normalizeType(warehouse.getType()));
        checkPartner(warehouse);
        if (warehouse.getStatus() == null) warehouse.setStatus(1);
        if (warehouse.getSort() == null) warehouse.setSort(0);
        warehouseMapper.insert(warehouse);
    }

    public void update(Long id, WmsWarehouse warehouse) {
        get(id);
        checkCodeUnique(warehouse.getCode(), id);
        warehouse.setId(id);
        warehouse.setType(normalizeType(warehouse.getType()));
        checkPartner(warehouse);
        if (warehouse.getSort() == null) warehouse.setSort(0);
        warehouse.setStatus(null);
        warehouseMapper.updateById(warehouse);
        warehouseMapper.update(Wrappers.<WmsWarehouse>lambdaUpdate().set(WmsWarehouse::getPartnerId, warehouse.getPartnerId())
                .eq(WmsWarehouse::getId, id));
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        WmsWarehouse entity = new WmsWarehouse();
        entity.setId(id);
        entity.setStatus(status);
        warehouseMapper.updateById(entity);
    }

    /** 已有库存或单据的仓库不能删除（可以停用），删除时一并删除其库位 */
    @Transactional
    public void delete(List<Long> ids) {
        for (WmsWarehouse warehouse : warehouseMapper.selectByIds(ids)) {
            Long id = warehouse.getId();
            boolean used = stockMapper.exists(Wrappers.<WmsStock>lambdaQuery().eq(WmsStock::getWarehouseId, id))
                    || docMapper.exists(Wrappers.<WmsStockDoc>lambdaQuery()
                    .and(w -> w.eq(WmsStockDoc::getWarehouseId, id).or().eq(WmsStockDoc::getToWarehouseId, id)))
                    || checkMapper.exists(Wrappers.<WmsCheck>lambdaQuery().eq(WmsCheck::getWarehouseId, id));
            if (used) {
                throw new BizException("wms.error.warehouseHasStockDocumentsCannotDeleted", warehouse.getName());
            }
            referenceGuard.checkDeletable(RefType.WAREHOUSE, id, Messages.get("wms.text.warehouse", warehouse.getName()));
        }
        locationMapper.delete(Wrappers.<WmsLocation>lambdaQuery().in(WmsLocation::getWarehouseId, ids));
        warehouseMapper.deleteByIds(ids);
    }

    private void checkCodeUnique(String code, Long id) {
        WmsWarehouse exist = warehouseMapper.selectOne(Wrappers.<WmsWarehouse>lambdaQuery().eq(WmsWarehouse::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("wms.error.warehouseCodeAlreadyExists");
        }
    }

    /** 未选择仓库类型时为普通仓 */
    private static WarehouseType normalizeType(WarehouseType type) {
        return type == null ? WarehouseType.NORMAL : type;
    }

    /** 寄售仓必须指定客户（客户寄售仓）/ 供应商（供应商寄售仓），其他仓库不关联往来单位 */
    private static void checkPartner(WmsWarehouse w) {
        if (w.getType().isConsign()) {
            if (w.getPartnerId() == null) {
                if (w.getType() == WarehouseType.CONSIGN_OUT) {
                    throw new BizException("wms.error.selectCustomerCustomerConsignmentWarehouse");
                }
                throw new BizException("wms.error.selectSupplierSupplierConsignmentWarehouse");
            }
        } else {
            w.setPartnerId(null);
        }
    }
}
