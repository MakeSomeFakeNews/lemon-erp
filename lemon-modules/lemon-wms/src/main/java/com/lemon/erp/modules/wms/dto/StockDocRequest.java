package com.lemon.erp.modules.wms.dto;

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
 */
public record StockDocRequest(
        String bizType,
        @NotNull(message = "请选择仓库") Long warehouseId,
        Long toWarehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加明细") @Size(max = 500, message = "明细不能超过500行") List<@Valid @NotNull StockDocItemRequest> items,
        Boolean inTransit) {

    /** 业务单据生成出入库单时使用（非在途） */
    public StockDocRequest(String bizType, Long warehouseId, Long toWarehouseId, LocalDate bizDate, String remark,
                           List<StockDocItemRequest> items) {
        this(bizType, warehouseId, toWarehouseId, bizDate, remark, items, null);
    }
}
