package com.lemon.erp.modules.scm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新增 / 修改发运单
 *
 * @param tradeDocId 销售出库单，修改时忽略
 * @param carrier    承运商，字典 scm_carrier
 * @param carrierSupplierId 承运商对应的供应商：有运费时发货生成运费应付单，为空不生成
 */
public record ShipmentRequest(
        @NotNull(message = "请选择销售出库单") Long tradeDocId,
        @NotBlank(message = "请选择承运商") @Size(max = 32) String carrier,
        @Size(max = 64, message = "运单号不能超过64个字符") String trackingNo,
        @DecimalMin(value = "0", message = "运费不能小于0") BigDecimal freight,
        @Size(max = 64, message = "收货人不能超过64个字符") String receiver,
        @Size(max = 32, message = "收货电话不能超过32个字符") String phone,
        @Size(max = 255, message = "收货地址不能超过255个字符") String address,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        Long carrierSupplierId) {
}
