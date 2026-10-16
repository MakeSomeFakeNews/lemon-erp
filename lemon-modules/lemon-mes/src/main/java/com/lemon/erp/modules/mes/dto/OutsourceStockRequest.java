package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 委外发料（委外发料出库单）/ 委外退料（委外收回入库单）
 */
public record OutsourceStockRequest(
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择日期") LocalDate bizDate,
        @NotEmpty(message = "请添加物料") @Size(max = 100, message = "明细不能超过100行") List<@Valid @NotNull StockDocItemRequest> items,
        @Size(max = 255, message = "备注不能超过255个字符") String remark) {
}
