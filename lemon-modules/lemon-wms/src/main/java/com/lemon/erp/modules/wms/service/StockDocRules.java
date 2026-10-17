package com.lemon.erp.modules.wms.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockChange;
import com.lemon.erp.modules.wms.model.StockDocType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.WarehouseType;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.lemon.erp.modules.wms.model.StockBizType.IN;
import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;
import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 出入库单的规则（纯计算，不访问数据库）：单据类型、寄售仓、批次、日期、序列号、冲销条件，以及明细对应的库存变动
 */
public final class StockDocRules {

    /** 序列号的最大长度 */
    private static final int MAX_SERIAL_LENGTH = 64;

    private StockDocRules() {
    }

    /** 手工入库只能是其他入库、寄售入库，手工出库只能是其他出库、报废出库：采购、销售、生产、项目等类型由对应的业务单据生成 */
    public static void checkManualType(StockBizType kind, String bizType) {
        if (kind == TRANSFER || bizType == null) {
            return;
        }
        StockDocType type = StockDocType.of(kind, bizType);
        if (type == null || !type.isManual()) {
            if (kind == IN) {
                throw new BizException("wms.error.manualInboundTypeNotAllowed");
            }
            throw new BizException("wms.error.manualOutboundTypeNotAllowed");
        }
    }

    /** 入库、出库必须选择有效的业务类型；调拨没有业务类型 */
    public static void checkBizType(StockBizType kind, String bizType) {
        if (kind != TRANSFER && !StringUtils.hasText(bizType)) {
            if (kind == IN) {
                throw new BizException("wms.error.selectInboundType");
            }
            throw new BizException("wms.error.selectOutboundType");
        }
        if (kind != TRANSFER && StockDocType.of(kind, bizType) == null) {
            if (kind == IN) {
                throw new BizException("wms.error.invalidInboundType", bizType);
            }
            throw new BizException("wms.error.invalidOutboundType", bizType);
        }
    }

    /** 供应商寄售仓（VMI）只能做寄售入库，寄售入库只能入供应商寄售仓 */
    public static void checkConsignWarehouse(StockBizType kind, String bizType, WarehouseType warehouseType) {
        boolean consignIn = StockInType.CONSIGN_IN.matches(bizType);
        if (kind == IN && consignIn != (warehouseType == WarehouseType.CONSIGN_IN)) {
            if (consignIn) {
                throw new BizException("wms.error.consignmentInboundRequiresSupplierConsignmentWarehouse");
            }
            throw new BizException("wms.error.supplierConsignmentWarehousesOnlyAllowConsignment");
        }
    }

    /** 调拨的调出、调入仓库和库位不能完全相同 */
    public static void checkTransferTarget(Long warehouseId, Long toWarehouseId, WmsStockDocItem item) {
        if (warehouseId.equals(toWarehouseId) && item.getLocationId().equals(item.getToLocationId())) {
            throw new BizException("wms.error.sourceDestinationWarehouseLocationSame");
        }
    }

    /** 未启用批次管理的物料清空批次号和日期；启用的必须填写批次号 */
    public static void normalizeBatch(WmsStockDocItem item, WmsMaterial material) {
        if (!material.isBatchManaged()) {
            item.setBatchNo(NO_BATCH);
            item.setProductionDate(null);
            item.setExpiryDate(null);
        } else if (item.getBatchNo().isEmpty()) {
            throw new BizException("wms.error.materialBatchManagedEnterBatchNo");
        }
    }

    /** 有效期至不能早于生产日期 */
    public static void checkDates(WmsStockDocItem item) {
        if (item.getProductionDate() != null && item.getExpiryDate() != null
                && item.getExpiryDate().isBefore(item.getProductionDate())) {
            throw new BizException("wms.error.expiryDateCannotEarlierProductionDate");
        }
    }

    /**
     * 校验序列号：数量为整数、个数与数量一致、同一物料在单据内不重复。非序列号物料忽略序列号
     *
     * @param strict 确认时为 true：序列号个数必须与数量一致；草稿只要求不超过数量
     * @param seen   该物料在本单据前面行中出现过的序列号
     * @return 规范后的序列号
     */
    public static List<String> checkSerials(String text, BigDecimal qty, WmsMaterial material, boolean strict, Set<String> seen) {
        if (!material.isSerialManaged()) {
            return List.of();
        }
        if (qty.stripTrailingZeros().scale() > 0) {
            throw new BizException("wms.error.quantitySerialManagedMaterialsMustInteger");
        }
        List<String> serials = splitSerials(text);
        int count = qty.intValue();
        if (strict ? serials.size() != count : serials.size() > count) {
            throw new BizException("wms.error.numberSerialNumbersDoesNotMatch", serials.size(), count);
        }
        for (String sn : serials) {
            if (sn.length() > MAX_SERIAL_LENGTH) {
                throw new BizException("wms.error.serialNoCannotExceedCharacters", sn);
            }
            if (!seen.add(sn)) {
                throw new BizException("wms.error.duplicateSerialNo", sn);
            }
        }
        return serials;
    }

    /**
     * 冲销条件：只能冲销已完成的手工单据，红字单据不能再冲销。
     * 业务单据生成的出入库须在业务单据中办理退货；在途调拨须先收货
     */
    public static void checkReversible(WmsStockDoc doc) {
        if (doc.getSourceNo() != null) {
            throw new BizException("wms.error.generatedBusinessDocumentProcessReturnDocument", doc.getSourceNo());
        }
        if (doc.getRedOf() != null) {
            throw new BizException("wms.error.redDocumentCannotReversed");
        }
        if (doc.getStatus() != DocStatus.DONE) {
            if (doc.getStatus() == DocStatus.TRANSIT) {
                throw new BizException("wms.error.receiveTransitTransferBeforeReversing");
            }
            throw new BizException("wms.error.onlyCompletedDocumentsReversed");
        }
    }

    /** 明细对应的库存变动：入库增加、出库减少，调拨先从调出库位减少再在调入库位增加 */
    public static List<StockChange> toChanges(StockBizType kind, WmsStockDoc doc, List<WmsStockDocItem> items) {
        List<StockChange> changes = new ArrayList<>();
        for (WmsStockDocItem item : items) {
            List<String> serials = splitSerials(item.getSerialNos());
            switch (kind) {
                case IN -> changes.add(new StockChange(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                        item.getBatchNo(), item.getQty(), serials, item.getProductionDate(), item.getExpiryDate()));
                case OUT -> changes.add(StockChange.of(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                        item.getBatchNo(), item.getQty().negate(), serials));
                case TRANSFER -> {
                    changes.add(StockChange.of(doc.getWarehouseId(), item.getLocationId(), item.getMaterialId(),
                            item.getBatchNo(), item.getQty().negate(), serials));
                    changes.add(StockChange.of(doc.getToWarehouseId(), item.getToLocationId(), item.getMaterialId(),
                            item.getBatchNo(), item.getQty(), serials));
                }
                default -> throw new IllegalArgumentException("不支持的单据类型：" + kind); // i18n-ignore 内部错误
            }
        }
        return changes;
    }

    /** 冲销的库存变动：原单变动反向（不再登记批次日期） */
    public static List<StockChange> reverseChanges(StockBizType kind, WmsStockDoc doc, List<WmsStockDocItem> items) {
        return toChanges(kind, doc, items).stream()
                .map(c -> StockChange.of(c.warehouseId(), c.locationId(), c.materialId(), c.batchNo(), c.qty().negate(), c.serialNos()))
                .toList();
    }
}
