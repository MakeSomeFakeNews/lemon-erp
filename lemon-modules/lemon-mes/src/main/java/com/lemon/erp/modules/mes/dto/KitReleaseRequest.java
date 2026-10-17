package com.lemon.erp.modules.mes.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 缺料下单：按生产用料汇总的建议数量，采购件 / 委外件按默认供应商合并为草稿采购订单，自制件生成草稿工单
 */
public record KitReleaseRequest(
        @NotEmpty(message = "{mes.valid.selectShortageMaterials}") @Size(max = 200, message = "{mes.valid.mostMaterialsTime}") List<@NotNull Long> materialIds,
        @NotNull(message = "{mes.valid.selectReceivingWarehouse}") Long warehouseId) {
}
