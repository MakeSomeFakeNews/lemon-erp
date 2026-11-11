package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.modules.wms.dto.TrackingRequest;
import com.lemon.erp.modules.wms.dto.TrackingView;
import com.lemon.erp.modules.wms.entity.WmsBatch;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsSerial;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.entity.WmsStockHold;
import com.lemon.erp.modules.wms.mapper.WmsBatchMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialCostMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsSerialMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockHoldMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.mapper.WmsTrackingMapper;
import com.lemon.erp.modules.wms.model.HoldStatus;
import com.lemon.erp.modules.wms.model.SerialStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;

/**
 * 物料批次 / 序列号管理的启用、停用（发生过库存业务之后）。
 * <ul>
 *   <li><b>启用批次管理</b>：已有库存没有批次号，按库存位置（仓库 + 库位）整体归入填写的批次（如「期初」），并登记该批次
 *       （可填生产日期、有效期）；在库序列号同步归入该批次。之后出入库必须填写批次号，出库时可选择该批次</li>
 *   <li><b>停用批次管理</b>：各批次的库存按库存位置合并为无批次（数量、冻结数量相加），在库序列号的批次清空；
 *       历史流水、序列号轨迹、批次登记保留原批次号，批次追溯照常查询。之后出入库不再要求批次</li>
 *   <li>两者都不改变任何库存位置的数量合计，不产生库存流水和成本变动</li>
 *   <li><b>序列号管理</b>只能在库存为 0 时切换：启用需要为每件在库物品登记序列号（相当于一次盘点），停用会使在库序列号的状态失真，
 *       都应先出库 / 盘亏到 0 再切换</li>
 *   <li>进行中的单据按已确定的批次、序列号执行（在途调拨收货、审批中的单据、盘点、待检 / 不合格冻结、发货通知拣货等），
 *       有这些单据时不能切换；草稿单据在确认时按物料当前的管理方式重新校验（启用后须补填批次号，停用后自动清空）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class WmsMaterialTrackingService {

    /** 提示中最多列出的进行中单据数 */
    private static final int MAX_LISTED = 5;

    private final WmsMaterialMapper materialMapper;
    private final WmsStockMapper stockMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsStockHoldMapper holdMapper;
    private final WmsSerialMapper serialMapper;
    private final WmsBatchMapper batchMapper;
    private final WmsMaterialCostMapper costMapper;
    private final WmsTrackingMapper trackingMapper;
    private final ChangeLogger changeLogger;
    private final List<MaterialTrackingGuard> guards;

    /** 切换前检查：当前库存情况和阻止切换的进行中单据 */
    public TrackingView preview(Long id) {
        WmsMaterial material = get(id);
        List<WmsStock> stocks = stocks(id);
        BigDecimal total = Decimals.sum(stocks, WmsStock::getQty);
        BigDecimal noBatch = Decimals.sum(stocks.stream().filter(s -> NO_BATCH.equals(s.getBatchNo())).toList(), WmsStock::getQty);
        long batches = stocks.stream().filter(s -> !NO_BATCH.equals(s.getBatchNo()) && s.getQty().signum() != 0)
                .map(WmsStock::getBatchNo).distinct().count();
        return new TrackingView(material.getBatchEnabled(), material.getSerialEnabled(), flowMapper.existsByMaterialId(id),
                total, noBatch, batches, openDocs(id));
    }

    @Transactional
    public void change(Long id, TrackingRequest req) {
        WmsMaterial before = get(id);
        if (before.isTemplateMaterial()) {
            throw new BizException("wms.error.trackingStyleNotAllowed");
        }
        int batch = Integer.valueOf(1).equals(req.batchEnabled()) ? 1 : 0;
        int serial = Integer.valueOf(1).equals(req.serialEnabled()) ? 1 : 0;
        boolean batchChanged = batch != (before.isBatchManaged() ? 1 : 0);
        boolean serialChanged = serial != (before.isSerialManaged() ? 1 : 0);
        if (!batchChanged && !serialChanged) {
            return;
        }
        // 与库存变动（StockService.apply 先按物料锁成本记录）互斥
        costMapper.selectForUpdate(List.of(id));
        List<String> open = openDocs(id);
        if (!open.isEmpty()) {
            throw new BizException("wms.error.trackingOpenDocs", String.join(Messages.get("wms.text.listSeparator"), open));
        }
        List<WmsStock> stocks = stocks(id);
        if (serialChanged && stocks.stream().anyMatch(s -> s.getQty().signum() != 0)) {
            throw new BizException("wms.error.serialSwitchNeedsZeroStock",
                    Decimals.sum(stocks, WmsStock::getQty).stripTrailingZeros().toPlainString());
        }
        if (batchChanged) {
            if (batch == 1) {
                enableBatch(id, stocks, req);
            } else {
                disableBatch(id, stocks);
            }
        }
        materialMapper.update(Wrappers.<WmsMaterial>lambdaUpdate()
                .set(WmsMaterial::getBatchEnabled, batch)
                .set(WmsMaterial::getSerialEnabled, serial)
                .set(WmsMaterial::getUpdateTime, LocalDateTime.now())
                .eq(WmsMaterial::getId, id));
        changeLogger.log("wms_material", id, before, materialMapper.selectById(id), WmsMaterialService.TRACKING_FIELDS);
    }

    /** 已有的无批次库存（按库存位置）归入指定批次，在库序列号同步 */
    private void enableBatch(Long materialId, List<WmsStock> stocks, TrackingRequest req) {
        List<WmsStock> noBatch = stocks.stream().filter(s -> NO_BATCH.equals(s.getBatchNo())).toList();
        boolean hasStock = noBatch.stream().anyMatch(s -> s.getQty().signum() != 0 || s.getFrozenQty().signum() != 0);
        String batchNo = req.batchNo() == null ? "" : req.batchNo().trim();
        if (hasStock && batchNo.isEmpty()) {
            throw new BizException("wms.error.trackingBatchNoRequired");
        }
        if (req.productionDate() != null && req.expiryDate() != null && req.expiryDate().isBefore(req.productionDate())) {
            throw new BizException("wms.error.expiryDateCannotEarlierProductionDate");
        }
        merge(stocks, noBatch, batchNo);
        if (!hasStock) {
            return;
        }
        serialMapper.update(Wrappers.<WmsSerial>lambdaUpdate()
                .set(WmsSerial::getBatchNo, batchNo)
                .eq(WmsSerial::getMaterialId, materialId)
                .eq(WmsSerial::getStatus, SerialStatus.IN_STOCK)
                .eq(WmsSerial::getBatchNo, NO_BATCH));
        WmsBatch exist = batchMapper.selectOne(Wrappers.<WmsBatch>lambdaQuery()
                .eq(WmsBatch::getMaterialId, materialId).eq(WmsBatch::getBatchNo, batchNo));
        if (exist == null) {
            WmsBatch b = new WmsBatch();
            b.setMaterialId(materialId);
            b.setBatchNo(batchNo);
            b.setProductionDate(req.productionDate());
            b.setExpiryDate(req.expiryDate());
            batchMapper.insert(b);
        } else if (req.productionDate() != null || req.expiryDate() != null) {
            batchMapper.update(Wrappers.<WmsBatch>lambdaUpdate()
                    .set(req.productionDate() != null, WmsBatch::getProductionDate, req.productionDate())
                    .set(req.expiryDate() != null, WmsBatch::getExpiryDate, req.expiryDate())
                    .eq(WmsBatch::getId, exist.getId()));
        }
    }

    /** 各批次的库存按库存位置合并为无批次，在库序列号的批次清空；历史流水、批次登记不变 */
    private void disableBatch(Long materialId, List<WmsStock> stocks) {
        merge(stocks, stocks.stream().filter(s -> !NO_BATCH.equals(s.getBatchNo())).toList(), NO_BATCH);
        serialMapper.update(Wrappers.<WmsSerial>lambdaUpdate()
                .set(WmsSerial::getBatchNo, NO_BATCH)
                .eq(WmsSerial::getMaterialId, materialId)
                .eq(WmsSerial::getStatus, SerialStatus.IN_STOCK)
                .ne(WmsSerial::getBatchNo, NO_BATCH));
    }

    /**
     * 把 sources 中的库存按库存位置（仓库 + 库位）并入批次 target：该位置已有 target 批次的记录时数量相加，
     * 否则把第一条来源改为 target 批次并加上其余的数量；其余来源删除。没有数量的来源直接删除
     *
     * @param all 物料的全部库存记录（查找已有的 target 记录）
     */
    private void merge(List<WmsStock> all, List<WmsStock> sources, String target) {
        Map<String, WmsStock> targets = all.stream().filter(s -> target.equals(s.getBatchNo()))
                .collect(Collectors.toMap(WmsMaterialTrackingService::slot, s -> s, (a, b) -> a));
        Map<String, List<WmsStock>> bySlot = sources.stream()
                .collect(Collectors.groupingBy(WmsMaterialTrackingService::slot, LinkedHashMap::new, Collectors.toList()));
        List<Long> deleted = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, List<WmsStock>> e : bySlot.entrySet()) {
            List<WmsStock> rows = new ArrayList<>();
            for (WmsStock s : e.getValue()) {
                if (s.getQty().signum() != 0 || s.getFrozenQty().signum() != 0) {
                    rows.add(s);
                } else {
                    deleted.add(s.getId());
                }
            }
            if (rows.isEmpty()) {
                continue;
            }
            WmsStock into = targets.get(e.getKey());
            BigDecimal qty = Decimals.sum(rows, WmsStock::getQty);
            BigDecimal frozen = Decimals.sum(rows, WmsStock::getFrozenQty);
            if (into == null) {
                into = rows.remove(0);
                qty = qty.subtract(into.getQty());
                frozen = frozen.subtract(into.getFrozenQty());
            }
            rows.forEach(s -> deleted.add(s.getId()));
            stockMapper.update(Wrappers.<WmsStock>lambdaUpdate()
                    .set(WmsStock::getBatchNo, target)
                    .set(WmsStock::getQty, into.getQty().add(qty))
                    .set(WmsStock::getFrozenQty, into.getFrozenQty().add(frozen))
                    .set(WmsStock::getUpdateTime, now)
                    .eq(WmsStock::getId, into.getId()));
        }
        if (!deleted.isEmpty()) {
            stockMapper.deleteByIds(deleted);
        }
    }

    private static String slot(WmsStock s) {
        return s.getWarehouseId() + "|" + s.getLocationId();
    }

    /** 阻止切换的进行中单据：仓库的在途调拨 / 审批中单据、未完成的盘点、仍冻结的待检 / 不合格库存，以及其他模块登记的 */
    private List<String> openDocs(Long materialId) {
        List<String> docs = new ArrayList<>();
        trackingMapper.selectOpenStockDocs(materialId).forEach(no -> docs.add(Messages.get("wms.text.openStockDoc", no)));
        trackingMapper.selectOpenChecks(materialId).forEach(no -> docs.add(Messages.get("wms.text.openCheck", no)));
        holdMapper.selectList(Wrappers.<WmsStockHold>lambdaQuery()
                        .select(WmsStockHold::getSourceNo, WmsStockHold::getQty)
                        .eq(WmsStockHold::getMaterialId, materialId)
                        .in(WmsStockHold::getStatus, HoldStatus.HOLDING)
                        .gt(WmsStockHold::getQty, BigDecimal.ZERO))
                .stream().map(h -> Objects.toString(h.getSourceNo(), "-")).distinct()
                .forEach(no -> docs.add(Messages.get("wms.text.openHold", no)));
        guards.forEach(g -> docs.addAll(g.openDocs(materialId)));
        if (docs.size() > MAX_LISTED) {
            int more = docs.size() - MAX_LISTED;
            List<String> listed = new ArrayList<>(docs.subList(0, MAX_LISTED));
            listed.add(Messages.get("wms.text.andMore", more));
            return listed;
        }
        return docs;
    }

    private List<WmsStock> stocks(Long materialId) {
        return stockMapper.selectList(Wrappers.<WmsStock>lambdaQuery().eq(WmsStock::getMaterialId, materialId)
                .orderByAsc(WmsStock::getId));
    }

    private WmsMaterial get(Long id) {
        WmsMaterial material = id == null ? null : materialMapper.selectById(id);
        if (material == null) {
            throw new BizException("wms.error.materialNotFound");
        }
        return material;
    }
}
