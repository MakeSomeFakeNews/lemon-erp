package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsBatch;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsBatchMapper;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.lemon.erp.modules.wms.model.StockBizType.IN;
import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;

/**
 * 出入库单的校验：单据类型、收发类别、仓库、库位、物料、批次、序列号、过期批次。纯规则见 {@link StockDocRules}
 */
@Component
@RequiredArgsConstructor
public class StockDocValidator {

    private final WmsReferenceLoader refs;
    private final DictLookup dictLookup;
    private final WmsBatchMapper batchMapper;

    /**
     * 收发类别：其他入库按字典 wms_in_category、其他出库和报废出库按字典 wms_out_category 校验（当前租户的业务字典），
     * 其他类型不保存。可不填
     */
    public String ioCategory(StockBizType kind, StockDocRequest req) {
        StockDocType type = StockDocType.of(kind, req.bizType());
        String dict = type == null ? null : type.ioCategoryDict();
        if (dict == null || !StringUtils.hasText(req.ioCategory())) {
            return null;
        }
        String value = req.ioCategory().trim();
        dictLookup.check(dict, value, kind == IN ? "wms.label.inCategory" : "wms.label.outCategory");
        return value;
    }

    /**
     * 校验单据，并按物料的管理方式规范明细（未启用批次管理的批次号置空等）
     *
     * @param strict 确认时为 true：序列号个数必须与数量一致；草稿只要求不超过数量
     */
    public void validate(StockBizType kind, String bizType, Long warehouseId, Long toWarehouseId,
                         List<WmsStockDocItem> items, boolean strict) {
        StockDocRules.checkBizType(kind, bizType);
        if (items.isEmpty()) {
            throw new BizException("wms.valid.addLineItems");
        }
        // 出库、调出允许停用的仓库和物料（清空剩余库存），入库、调入要求启用
        WmsWarehouse warehouse = refs.warehouse(warehouseId, kind == TRANSFER ? "wms.label.sourceWarehouse" : "wms.label.warehouse", kind == IN);
        WmsWarehouse toWarehouse = kind == TRANSFER ? refs.warehouse(toWarehouseId, "wms.label.destinationWarehouse") : null;
        StockDocRules.checkConsignWarehouse(kind, bizType, warehouse.getType());
        Map<Long, WmsMaterial> materials = refs.materials(items.stream().map(WmsStockDocItem::getMaterialId).toList(), kind == IN);
        Map<Long, WmsLocation> locations = refs.locations(items.stream()
                .flatMap(i -> Stream.of(i.getLocationId(), i.getToLocationId())).toList());
        Map<Long, Set<String>> serialsByMaterial = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            WmsStockDocItem item = items.get(i);
            WmsMaterial material = materials.get(item.getMaterialId());
            try {
                // 入库、调入要求库位启用；出库、调出允许从停用的库位取出
                WmsReferenceLoader.checkLocation(locations, item.getLocationId(), warehouse, kind == IN);
                if (kind == TRANSFER) {
                    WmsReferenceLoader.checkLocation(locations, item.getToLocationId(), toWarehouse, true);
                    StockDocRules.checkTransferTarget(warehouse.getId(), toWarehouse.getId(), item);
                }
                StockDocRules.normalizeBatch(item, material);
                StockDocRules.checkDates(item);
                List<String> serials = StockDocRules.checkSerials(item.getSerialNos(), item.getQty(), material, strict,
                        serialsByMaterial.computeIfAbsent(material.getId(), k -> new HashSet<>()));
                item.setSerialNos(joinSerials(serials));
            } catch (BizException e) {
                throw new BizException("wms.error.line", i + 1, StockService.describe(material), e.getLocalizedMessage());
            }
        }
    }

    /** 销售、生产、项目等出库不能发出已过期的批次（可报废出库或调拨到不良品仓） */
    public void checkNotExpired(List<WmsStockDocItem> items, LocalDate bizDate) {
        for (WmsStockDocItem item : items) {
            if (item.getBatchNo() == null || item.getBatchNo().isEmpty()) {
                continue;
            }
            WmsBatch batch = batchMapper.selectOne(Wrappers.<WmsBatch>lambdaQuery().eq(WmsBatch::getMaterialId, item.getMaterialId())
                    .eq(WmsBatch::getBatchNo, item.getBatchNo()));
            if (batch != null && batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(bizDate)) {
                throw new BizException("wms.error.batchExpiredCannotIssuedScrapTransfer", item.getBatchNo(), batch.getExpiryDate());
            }
        }
    }

    /** 在途调拨收货：调入仓库须启用 */
    public void checkReceiveWarehouse(WmsStockDoc doc) {
        refs.warehouse(doc.getToWarehouseId(), "wms.label.destinationWarehouse", true);
    }
}
