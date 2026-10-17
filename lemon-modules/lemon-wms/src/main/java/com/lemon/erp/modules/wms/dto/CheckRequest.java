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
        @NotNull(message = "{wms.valid.selectCountWarehouse}") Long warehouseId,
        @NotNull(message = "{wms.valid.selectCountDate}") LocalDate bizDate,
        @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{wms.valid.addCountItems}") @Size(max = 2000, message = "{wms.valid.lineItemsCannotExceed}") List<@Valid @NotNull CheckItemRequest> items,
        Boolean freeze) {
}
