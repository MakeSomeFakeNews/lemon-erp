package com.lemon.erp.modules.pm.dto;

import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目领料：生成「项目领料」出库单并直接确认，按存货单位成本计入项目材料成本
 */
public record IssueRequest(
        @NotNull(message = "请选择仓库") Long warehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加领料明细") @Size(max = 200, message = "明细不能超过200行") List<@Valid @NotNull StockDocItemRequest> items) {
}
