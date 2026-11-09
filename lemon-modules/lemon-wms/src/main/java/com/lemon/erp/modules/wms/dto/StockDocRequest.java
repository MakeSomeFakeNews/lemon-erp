package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.modules.wms.model.StockDocType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改出入库单（入库、出库、调拨）
 *
 * @param bizType       入库类型（字典 wms_in_type）/ 出库类型（字典 wms_out_type），调拨不填
 * @param warehouseId   仓库；调拨为调出仓库
 * @param toWarehouseId 调入仓库，仅调拨
 * @param ioCategory    收发类别：其他入库为字典 wms_in_category，其他出库、报废出库为字典 wms_out_category，可不填；其他类型忽略
 * @param partnerId     往来单位（供应商 / 客户），业务单据生成时传入，记录在单据和库存流水上；手工单据忽略
 * @param partnerName   往来单位名称快照
 */
public record StockDocRequest(
        String bizType,
        @NotNull(message = "{wms.valid.selectWarehouse}") Long warehouseId,
        Long toWarehouseId,
        @NotNull(message = "{wms.valid.selectBusinessDate}") LocalDate bizDate,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{wms.valid.addLineItems}") @Size(max = 500, message = "{wms.valid.lineItemsCannotExceed2}") List<@Valid @NotNull StockDocItemRequest> items,
        Boolean inTransit,
        String ioCategory,
        Long partnerId,
        String partnerName) {

    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items, Boolean inTransit, String ioCategory) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, inTransit, ioCategory, null, null);
    }

    /** 业务单据生成出入库单时使用（非在途） */
    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, null, null);
    }

    /** 业务单据生成出入库单时使用（非在途），业务类型取入库类型 / 出库类型枚举 */
    public StockDocRequest(StockDocType bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items) {
        this(bizType.name(), warehouseId, toWarehouseId, bizDate, remark, items, null, null);
    }

    /** 带上往来单位（记录在单据和库存流水上） */
    public StockDocRequest withPartner(Long partnerId, String partnerName) {
        return new StockDocRequest(bizType, warehouseId, toWarehouseId, bizDate, remark, items, inTransit, ioCategory, partnerId, partnerName);
    }

    /** 替换明细（如带上成本单价） */
    public StockDocRequest withItems(List<StockDocItemRequest> newItems) {
        return new StockDocRequest(bizType, warehouseId, toWarehouseId, bizDate, remark, newItems, inTransit, ioCategory, partnerId, partnerName);
    }
}
