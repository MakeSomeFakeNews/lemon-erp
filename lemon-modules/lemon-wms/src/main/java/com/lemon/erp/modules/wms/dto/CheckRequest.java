package com.lemon.erp.modules.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改盘点单；账面数量由服务端按当前库存填写
 */
public record CheckRequest(
        @NotNull(message = "请选择盘点仓库") Long warehouseId,
        @NotNull(message = "请选择盘点日期") LocalDate bizDate,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        @NotEmpty(message = "请添加盘点明细") @Size(max = 2000, message = "明细不能超过2000行") List<@Valid @NotNull CheckItemRequest> items,
        Boolean freeze) {
}
