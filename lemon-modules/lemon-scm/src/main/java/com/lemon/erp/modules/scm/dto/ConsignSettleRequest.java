package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 寄售结算
 *
 * @param warehouseId       寄售仓（客户寄售仓：客户已售出的；供应商寄售仓：已消耗的）
 * @param targetWarehouseId 供应商寄售结算时转入的自有仓库
 */
public record ConsignSettleRequest(
        @NotNull(message = "请选择寄售仓") Long warehouseId,
        Long targetWarehouseId,
        @NotNull(message = "请选择业务日期") LocalDate bizDate,
        @Size(max = 255) String remark,
        @NotEmpty(message = "请添加结算明细") @Size(max = 200) List<@Valid @NotNull Item> items) {

    /**
     * @param price 含税单价（原币），为空时客户寄售取价目表、供应商寄售取供货价格
     */
    public record Item(
            @NotNull(message = "请选择物料") Long materialId,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4) BigDecimal qty,
            @DecimalMin(value = "0", message = "单价不能小于0") BigDecimal price,
            BigDecimal taxRate,
            Long locationId,
            @Size(max = 64) String batchNo,
            String serialNos) {
    }
}
