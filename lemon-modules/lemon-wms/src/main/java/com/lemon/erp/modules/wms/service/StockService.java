package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.wms.dto.SerialLog;
import com.lemon.erp.modules.wms.dto.StockFlow;
import com.lemon.erp.modules.wms.entity.WmsBatch;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsSerial;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsBatchMapper;
import com.lemon.erp.modules.wms.mapper.WmsCheckMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialLogMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.SerialStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存核心：所有库存变动的唯一入口。在单据确认的事务中执行，任一行失败整张单据回滚。
 * <ul>
 *   <li>库存：条件更新（扣减时 {@code qty - 冻结数 >= 扣减数}），并发下不会出现负库存，冻结（待检、不合格）的库存不能出库</li>
 *   <li>批次：首次入库时登记批次及生产日期、有效期</li>
 *   <li>序列号：入库登记 / 出库核销，校验是否在库、是否在该库存位置</li>
 *   <li>流水：每次变动记录库存流水与序列号轨迹，批量写入</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StockService {

    /** 先扣减后增加（调拨的序列号要先从调出位置出库）；同类变动按库存位置排序，减少并发确认时的死锁 */
    private static final Comparator<StockChange> APPLY_ORDER = Comparator
            .comparing(StockChange::increase)
            .thenComparing(StockChange::materialId)
            .thenComparing(StockChange::warehouseId)
            .thenComparingLong(StockChange::locationId)
            .thenComparing(StockChange::batchNo);

    private final WmsStockMapper stockMapper;
    private final WmsMaterialMapper materialMapper;
    private final WmsBatchMapper batchMapper;
    private final WmsSerialMapper serialMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsSerialLogMapper serialLogMapper;
    private final StockReservationService reservationService;
    private final WmsCheckMapper checkMapper;
    private final WmsWarehouseMapper warehouseMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(StockBizType bizType, Long docId, String docNo, List<StockChange> changes) {
        List<StockChange> sorted = changes.stream().filter(c -> c.qty().signum() != 0).sorted(APPLY_ORDER).toList();
        if (sorted.isEmpty()) {
            return;
        }
        if (bizType != StockBizType.CHECK) {
            checkNotFrozen(sorted.stream().map(StockChange::warehouseId).collect(Collectors.toSet()));
        }
        Map<Long, WmsMaterial> materials = materialMapper.selectByIds(sorted.stream().map(StockChange::materialId).distinct().toList())
                .stream().collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
        materials.values().stream().filter(WmsMaterial::isTemplateMaterial).findFirst().ifPresent(m -> {
            throw new BizException("wms.error.multiVariantStyleSelectSpecificVariant", m.getCode(), m.getName());
        });
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        List<StockFlow> flows = new ArrayList<>(sorted.size());
        List<SerialLog> serialLogs = new ArrayList<>();

        for (StockChange change : sorted) {
            WmsMaterial material = materials.get(change.materialId());
            if (change.increase()) {
                increase(change);
            } else {
                decrease(change, material);
            }
            if (!change.serialNos().isEmpty()) {
                applySerials(change, material);
                int direction = change.increase() ? 1 : -1;
                change.serialNos().forEach(sn -> serialLogs.add(new SerialLog(change.materialId(), sn, bizType, docId, docNo,
                        direction, change.warehouseId(), change.locationId(), change.batchNo(), userId, now)));
            }
            flows.add(new StockFlow(bizType, docId, docNo, change.warehouseId(), change.locationId(), change.materialId(),
                    change.batchNo(), change.qty(), userId, now));
        }
        if (bizType != StockBizType.CHECK) {
            // 盘亏是实物事实，不受预留限制；其余扣减不能占用已预留的库存
            Map<Long, Set<Long>> decreased = new HashMap<>();
            sorted.stream().filter(c -> !c.increase())
                    .forEach(c -> decreased.computeIfAbsent(c.warehouseId(), k -> new HashSet<>()).add(c.materialId()));
            if (!decreased.isEmpty()) {
                reservationService.checkAfterDecrease(decreased);
            }
        }
        registerBatches(sorted);
        flowMapper.insertBatch(flows);
        if (!serialLogs.isEmpty()) {
            serialLogMapper.insertBatch(serialLogs);
        }
    }

    /** 冻结仓库的盘点（未完成）期间，该仓库不能出入库、调拨 */
    private void checkNotFrozen(Set<Long> warehouseIds) {
        WmsCheck check = checkMapper.selectOne(Wrappers.<WmsCheck>lambdaQuery().in(WmsCheck::getWarehouseId, warehouseIds)
                .eq(WmsCheck::getFreeze, 1).in(WmsCheck::getStatus, DocStatus.DRAFT, DocStatus.APPROVING).last("LIMIT 1"));
        if (check != null) {
            WmsWarehouse w = warehouseMapper.selectById(check.getWarehouseId());
            throw new BizException("wms.error.warehouseCountedNoInboundOutboundTransfer",
                    w == null ? check.getWarehouseId() : w.getName(), check.getDocNo());
        }
    }

    private void increase(StockChange c) {
        if (stockMapper.increase(c.warehouseId(), c.locationId(), c.materialId(), c.batchNo(), c.qty()) > 0) {
            return;
        }
        WmsStock stock = new WmsStock();
        stock.setWarehouseId(c.warehouseId());
        stock.setLocationId(c.locationId());
        stock.setMaterialId(c.materialId());
        stock.setBatchNo(c.batchNo());
        stock.setQty(c.qty());
        stock.setFrozenQty(BigDecimal.ZERO);
        try {
            stockMapper.insert(stock);
        } catch (DuplicateKeyException e) {
            // 并发确认的另一张单据刚好插入了同一库存记录，改为累加
            stockMapper.increase(c.warehouseId(), c.locationId(), c.materialId(), c.batchNo(), c.qty());
        }
    }

    private void decrease(StockChange c, WmsMaterial material) {
        BigDecimal qty = c.qty().negate();
        if (stockMapper.decrease(c.warehouseId(), c.locationId(), c.materialId(), c.batchNo(), qty) == 0) {
            WmsStock stock = stockMapper.selectOne(Wrappers.<WmsStock>lambdaQuery()
                    .eq(WmsStock::getWarehouseId, c.warehouseId())
                    .eq(WmsStock::getLocationId, c.locationId())
                    .eq(WmsStock::getMaterialId, c.materialId())
                    .eq(WmsStock::getBatchNo, c.batchNo()));
            BigDecimal available = stock == null ? BigDecimal.ZERO : stock.getQty().subtract(stock.getFrozenQty());
            String frozen = stock == null || stock.getFrozenQty().signum() == 0 ? ""
                    : Messages.get("wms.text.moreFrozenPendingFailedInspection", stock.getFrozenQty().stripTrailingZeros().toPlainString());
            throw new BizException("wms.error.insufficientStockAvailableRequired", describe(material),
                    c.batchNo().isEmpty() ? "" : Messages.get("wms.text.batch", c.batchNo()),
                    available.stripTrailingZeros().toPlainString(), qty.stripTrailingZeros().toPlainString(), frozen);
        }
    }

    /** 入库：新序列号登记、已出库的序列号重新入库；出库：必须在该库存位置在库 */
    private void applySerials(StockChange c, WmsMaterial material) {
        Map<String, WmsSerial> existing = serialMapper.selectList(Wrappers.<WmsSerial>lambdaQuery()
                        .eq(WmsSerial::getMaterialId, c.materialId())
                        .in(WmsSerial::getSerialNo, c.serialNos()))
                .stream().collect(Collectors.toMap(WmsSerial::getSerialNo, Function.identity()));

        if (c.increase()) {
            List<Long> returned = new ArrayList<>();
            List<WmsSerial> created = new ArrayList<>();
            for (String sn : c.serialNos()) {
                WmsSerial serial = existing.get(sn);
                if (serial == null) {
                    WmsSerial s = new WmsSerial();
                    s.setMaterialId(c.materialId());
                    s.setSerialNo(sn);
                    created.add(s);
                } else if (serial.getStatus() == SerialStatus.IN_STOCK) {
                    throw new BizException("wms.error.serialNoAlreadyStock", describe(material), sn);
                } else {
                    returned.add(serial.getId());
                }
            }
            if (!created.isEmpty()) {
                created.forEach(s -> {
                    s.setWarehouseId(c.warehouseId());
                    s.setLocationId(c.locationId());
                    s.setBatchNo(c.batchNo());
                    s.setStatus(SerialStatus.IN_STOCK);
                });
                serialMapper.insertBatch(created);
            }
            if (!returned.isEmpty()) {
                serialMapper.updateSlot(returned, c.warehouseId(), c.locationId(), c.batchNo(), SerialStatus.IN_STOCK);
            }
            return;
        }

        List<Long> ids = new ArrayList<>(c.serialNos().size());
        for (String sn : c.serialNos()) {
            WmsSerial serial = existing.get(sn);
            boolean inSlot = serial != null
                    && serial.getStatus() == SerialStatus.IN_STOCK
                    && Objects.equals(serial.getWarehouseId(), c.warehouseId())
                    && serial.getLocationId() == c.locationId()
                    && Objects.equals(serial.getBatchNo(), c.batchNo());
            if (!inSlot) {
                throw new BizException("wms.error.serialNoNotStockLocation", describe(material), sn);
            }
            ids.add(serial.getId());
        }
        serialMapper.updateSlot(ids, c.warehouseId(), c.locationId(), c.batchNo(), SerialStatus.OUT);
    }

    /** 入库的批次如果还没有登记，登记批次（同一批次以第一行的生产日期、有效期为准） */
    private void registerBatches(List<StockChange> changes) {
        Map<String, StockChange> incoming = new LinkedHashMap<>();
        changes.stream().filter(c -> c.increase() && !c.batchNo().isEmpty())
                .forEach(c -> incoming.putIfAbsent(batchKey(c.materialId(), c.batchNo()), c));
        if (incoming.isEmpty()) {
            return;
        }
        Set<Long> materialIds = incoming.values().stream().map(StockChange::materialId).collect(Collectors.toSet());
        Set<String> batchNos = incoming.values().stream().map(StockChange::batchNo).collect(Collectors.toSet());
        batchMapper.selectList(Wrappers.<WmsBatch>lambdaQuery()
                        .select(WmsBatch::getMaterialId, WmsBatch::getBatchNo)
                        .in(WmsBatch::getMaterialId, materialIds)
                        .in(WmsBatch::getBatchNo, batchNos))
                .forEach(b -> incoming.remove(batchKey(b.getMaterialId(), b.getBatchNo())));
        if (incoming.isEmpty()) {
            return;
        }
        batchMapper.insertBatch(incoming.values().stream().map(c -> {
            WmsBatch batch = new WmsBatch();
            batch.setMaterialId(c.materialId());
            batch.setBatchNo(c.batchNo());
            batch.setProductionDate(c.productionDate());
            batch.setExpiryDate(c.expiryDate());
            return batch;
        }).toList());
    }

    private static String batchKey(Long materialId, String batchNo) {
        return materialId + "\n" + batchNo;
    }

    static String describe(WmsMaterial material) {
        return material == null ? "" : material.getCode() + " " + material.getName();
    }
}
